#!/usr/bin/env python3
"""Subtitle downloader using opensubtitles.com API directly"""

import sys
import os
import argparse
import json
import requests
import time


OPENSUBTITLESCOM_API_KEY = 'mij33pjc3kOlup1qOKxnWWxvle2kFbMH'
USER_AGENT = 'Subliminal v2.6.0'


def search_subtitles_by_imdb(imdb_id, languages):
    """Search subtitles by IMDB ID

    Args:
        imdb_id: IMDB ID (e.g., 'tt12345678')
        languages: List of language codes (e.g., ['en', 'zh'])

    Returns:
        List of subtitle results
    """
    url = "https://api.opensubtitles.com/api/v1/imdb"

    headers = {
        "Api-Key": OPENSUBTITLESCOM_API_KEY,
        "User-Agent": USER_AGENT,
    }

    params = {
        "imdb_id": imdb_id,
        "languages": ",".join(languages),
    }

    print(f"Searching subtitles: {url}")
    print(f"Params: {params}")

    try:
        response = requests.get(url, headers=headers, params=params, timeout=30)
        print(f"Response code: {response.status_code}")

        if response.status_code == 200:
            data = response.json()
            print(f"Response data: {json.dumps(data, indent=2)[:500]}")
            return data.get('data', [])
        else:
            print(f"Error response: {response.text}")
            return []
    except Exception as e:
        print(f"Error: {e}")
        return []


def download_subtitle(file_id, output_path):
    """Download subtitle file

    Args:
        file_id: File ID from search results
        output_path: Path to save the subtitle file

    Returns:
        True if successful, False otherwise
    """
    url = f"https://api.opensubtitles.com/api/v1/download"

    headers = {
        "Api-Key": OPENSUBTITLESCOM_API_KEY,
        "User-Agent": USER_AGENT,
    }

    data = {
        "file_id": file_id,
    }

    print(f"Downloading subtitle: {url}")
    print(f"File ID: {file_id}")

    try:
        response = requests.post(url, headers=headers, json=data, timeout=60)
        print(f"Response code: {response.status_code}")

        if response.status_code == 200:
            # Response contains the download link or file
            result = response.json()
            print(f"Download result: {json.dumps(result, indent=2)[:500]}")

            # If response contains a link, download from that link
            if 'link' in result:
                link = result['link']
                print(f"Downloading from link: {link}")
                file_response = requests.get(link, timeout=60)
                if file_response.status_code == 200:
                    with open(output_path, 'wb') as f:
                        f.write(file_response.content)
                    print(f"Saved to: {output_path}")
                    return True

            # If response contains direct content
            if 'content' in result:
                with open(output_path, 'wb') as f:
                    f.write(result['content'].encode('utf-8'))
                print(f"Saved to: {output_path}")
                return True

            # If response is the file directly
            if response.headers.get('content-type', '').startswith('application'):
                with open(output_path, 'wb') as f:
                    f.write(response.content)
                print(f"Saved to: {output_path}")
                return True

            return False
        else:
            print(f"Error response: {response.text}")
            return False
    except Exception as e:
        print(f"Error: {e}")
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

        # Convert timestamps (SRT format: 00:00:00,000 --> 00:00:00,000)
        # to VTT format: 00:00:00.000 --> 00:00:00.000
        def convert_timestamp(timestamp):
            # Replace comma with dot
            timestamp = timestamp.replace(',', '.')
            return timestamp

        # Process SRT blocks
        blocks = re.split(r'\n\s*\n', srt_content.strip())

        for block in blocks:
            lines = block.strip().split('\n')
            if len(lines) >= 2:
                # Skip the index line if it's just a number
                if lines[0].strip().isdigit():
                    lines = lines[1:]

                if len(lines) >= 2:
                    # Convert timestamp line
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

    # Search for subtitles
    results = search_subtitles_by_imdb(imdb_id, languages)

    if not results:
        print("No subtitles found")
        return []

    downloaded_files = []

    for result in results[:5]:  # Limit to 5 results
        # Get file info
        attributes = result.get('attributes', {})
        files = attributes.get('files', [])

        if not files:
            continue

        file_info = files[0]
        file_id = file_info.get('file_id')
        file_name = file_info.get('file_name', '')
        language = attributes.get('language', '')

        print(f"\nProcessing: {file_name} ({language})")

        # Determine output file paths
        base_name = os.path.splitext(file_name)[0] if file_name else f"{language}"
        srt_path = os.path.join(output_dir, f"{base_name}.srt")
        vtt_path = os.path.join(output_dir, f"{language}.vtt")

        # Download subtitle
        if download_subtitle(file_id, srt_path):
            # Convert to VTT
            if convert_srt_to_vtt(srt_path, vtt_path):
                downloaded_files.append(vtt_path)

            # Remove SRT file
            if os.path.exists(srt_path):
                os.remove(srt_path)

        # Rate limit
        time.sleep(1)

    return downloaded_files


def main():
    parser = argparse.ArgumentParser(description='Download subtitles')
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