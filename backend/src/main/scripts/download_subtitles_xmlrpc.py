#!/usr/bin/env python3
"""Subtitle downloader using OpenSubtitles XML-RPC API"""

import sys
import os
import argparse
import xmlrpc.client
import gzip
import base64


# OpenSubtitles XML-RPC server
SERVER_URL = 'https://api.opensubtitles.org/xml-rpc'


def login():
    """Login to OpenSubtitles

    Returns:
        (server, token) on success, (None, None) on failure
    """
    try:
        server = xmlrpc.client.ServerProxy(SERVER_URL, allow_none=True)
        # Try without login first (some servers allow anonymous)
        print("Connecting to OpenSubtitles XML-RPC...")
        response = server.LogIn('', '', 'en', 'Subliminal v1.0')
        print(f"Login response: {response}")
        if response.get('status') == '200':
            token = response.get('token')
            print(f"Login successful, token: {token[:20]}...")
            return server, token
        else:
            print(f"Login failed: {response}")
            return None, None
    except Exception as e:
        print(f"Login error: {e}")
        return None, None


def search_subtitles_by_imdb(server, token, imdb_id, languages):
    """Search subtitles by IMDB ID

    Args:
        server: XML-RPC server proxy
        token: Authentication token
        imdb_id: IMDB ID (e.g., 'tt12345678')
        languages: List of language codes

    Returns:
        List of subtitle results
    """
    print(f"Searching subtitles for IMDB: {imdb_id}, languages: {languages}")

    try:
        # Search by IMDB ID
        query = [{
            'imdbid': imdb_id.replace('tt', ''),  # Remove 'tt' prefix if present
            'sublanguageid': ','.join(languages),
        }]

        response = server.SearchSubtitles(token, query)
        print(f"Search response status: {response.get('status')}")

        if response.get('status') == '200':
            data = response.get('data')
            if data:
                print(f"Found {len(data)} subtitles")
                return data
            else:
                print("No subtitles found in data")
                return []
        else:
            print(f"Search failed: {response}")
            return []
    except Exception as e:
        print(f"Search error: {e}")
        return []


def download_subtitle(server, token, subtitle_data, output_path):
    """Download subtitle file

    Args:
        server: XML-RPC server proxy
        token: Authentication token
        subtitle_data: Subtitle data from search
        output_path: Path to save the subtitle file

    Returns:
        True if successful, False otherwise
    """
    print(f"Downloading subtitle: {subtitle_data.get('SubFileName')}")

    try:
        # Get the subtitle file
        response = server.DownloadSubtitles(token, [subtitle_data['IDSubtitleFile']])
        print(f"Download response status: {response.get('status')}")

        if response.get('status') == '200':
            data = response.get('data')
            if data and len(data) > 0:
                # Decode gzip content
                gzipped_content = base64.b64decode(data[0]['data'])
                content = gzip.decompress(gzipped_content).decode('utf-8', errors='replace')

                # Save to file
                with open(output_path, 'w', encoding='utf-8') as f:
                    f.write(content)

                print(f"Saved to: {output_path}")
                return True
            else:
                print("No data in download response")
                return False
        else:
            print(f"Download failed: {response}")
            return False
    except Exception as e:
        print(f"Download error: {e}")
        return False


def convert_srt_to_vtt(srt_path, vtt_path):
    """Convert SRT to VTT format

    Args:
        srt_path: Path to SRT file
        vtt_path: Path to output VTT file

    Returns:
        True if successful, False otherwise
    """
    import re

    print(f"Converting SRT to VTT: {srt_path} -> {vtt_path}")

    try:
        with open(srt_path, 'r', encoding='utf-8') as f:
            srt_content = f.read()

        # Add VTT header
        vtt_content = "WEBVTT\n\n"

        # Remove BOM if present
        srt_content = srt_content.lstrip('\ufeff')

        # Process SRT blocks
        blocks = re.split(r'\n\s*\n', srt_content.strip())

        for block in blocks:
            lines = block.strip().split('\n')
            if len(lines) >= 2:
                # Skip the index line if it's just a number
                if lines[0].strip().isdigit():
                    lines = lines[1:]

                if len(lines) >= 2:
                    # Convert timestamp line (replace comma with dot)
                    timestamp_line = lines[0].replace(',', '.')
                    vtt_content += timestamp_line + '\n'

                    # Add text lines
                    for text_line in lines[1:]:
                        vtt_content += text_line + '\n'

                    vtt_content += '\n'

        with open(vtt_path, 'w', encoding='utf-8') as f:
            f.write(vtt_content)

        print(f"Converted successfully")
        return True
    except Exception as e:
        print(f"Error converting: {e}")
        return False


def download_subtitles(imdb_id, title, languages, output_dir):
    """Download subtitles by IMDB ID

    Args:
        imdb_id: IMDB ID (e.g., 'tt12345678')
        title: Video title (unused, for logging)
        languages: List of language codes (e.g., ['en', 'zh'])
        output_dir: Output directory

    Returns:
        List of downloaded subtitle files
    """
    # Ensure output directory exists
    os.makedirs(output_dir, exist_ok=True)

    # Login to OpenSubtitles
    server, token = login()
    if not token:
        print("Failed to login to OpenSubtitles")
        return []

    try:
        # Search for subtitles
        results = search_subtitles_by_imdb(server, token, imdb_id, languages)

        if not results:
            print("No subtitles found")
            return []

        downloaded_files = []

        for result in results[:5]:  # Limit to 5 results
            sub_id = result.get('IDSubtitleFile')
            sub_filename = result.get('SubFileName', '')
            language = result.get('ISO639', '')
            language_name = result.get('LanguageName', '')

            print(f"\nProcessing: {sub_filename} ({language_name})")

            # Determine output file paths
            base_name = os.path.splitext(sub_filename)[0] if sub_filename else f"{language}"
            srt_path = os.path.join(output_dir, f"{base_name}.srt")
            vtt_path = os.path.join(output_dir, f"{language}.vtt")

            # Download subtitle
            if download_subtitle(server, token, result, srt_path):
                # Convert to VTT
                if convert_srt_to_vtt(srt_path, vtt_path):
                    downloaded_files.append(vtt_path)

                # Remove SRT file
                if os.path.exists(srt_path):
                    os.remove(srt_path)

        return downloaded_files

    finally:
        # Logout
        try:
            server.LogOut(token)
        except:
            pass


def main():
    parser = argparse.ArgumentParser(description='Download subtitles using OpenSubtitles XML-RPC')
    parser.add_argument('--imdb-id', required=True, help='IMDB ID (e.g., tt12345678)')
    parser.add_argument('--title', required=True, help='Video title')
    parser.add_argument('--languages', required=True, help='Comma-separated language codes (e.g., en,zh)')
    parser.add_argument('--output-dir', required=True, help='Output directory')

    args = parser.parse_args()

    languages = args.languages.split(',')
    print(f"Downloading subtitles for: {args.title} ({args.imdb_id})")
    print(f"Languages: {languages}")
    print(f"Output dir: {args.output_dir}")

    saved = download_subtitles(args.imdb_id, args.title, languages, args.output_dir)

    print(f"\nSaved files: {saved}")
    return 0 if saved else 1


if __name__ == '__main__':
    sys.exit(main())