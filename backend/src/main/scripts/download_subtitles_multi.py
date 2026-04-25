#!/usr/bin/env python3
"""Multi-source subtitle downloader"""

import sys
import os
import argparse
import json
import requests
import gzip
import base64
import time
import re

# OpenSubtitles.com API
OPENSUBTITLES_API_KEY = "mij33pjc3kOlup1qOKxnWWxvle2kFbMH"
OPENSUBTITLES_USER_AGENT = "Subliminal v2.6.0"


def search_opensubtitlescom(imdb_id, languages):
    """Search using opensubtitlescom API (free tier)"""
    url = f"https://api.opensubtitles.com/api/v1/imdb"

    headers = {
        "Api-Key": OPENSUBTITLES_API_KEY,
        "User-Agent": OPENSUBTITLES_USER_AGENT,
    }

    params = {
        "imdb_id": imdb_id,
        "languages": ",".join(languages),
    }

    try:
        response = requests.get(url, headers=headers, params=params, timeout=30)
        if response.status_code == 200:
            data = response.json()
            return data.get('data', [])
    except Exception as e:
        print(f"opensubtitlescom search error: {e}")

    return []


def search_podnapisi(imdb_id, languages):
    """Search using Podnapisi API"""
    # Podnapisi doesn't have a public API, skip
    return []


def download_from_link(link, output_path):
    """Download subtitle from direct link"""
    try:
        response = requests.get(link, timeout=60)
        if response.status_code == 200:
            with open(output_path, 'wb') as f:
                f.write(response.content)
            return True
    except Exception as e:
        print(f"Download from link error: {e}")
    return False


def convert_srt_to_vtt(srt_path, vtt_path):
    """Convert SRT to VTT format"""
    try:
        with open(srt_path, 'r', encoding='utf-8', errors='replace') as f:
            srt_content = f.read()

        vtt_content = "WEBVTT\n\n"
        srt_content = srt_content.lstrip('\ufeff')

        # Convert timestamps
        def convert_timestamp(ts):
            return ts.replace(',', '.')

        # Process blocks
        blocks = re.split(r'\n\s*\n', srt_content.strip())
        for block in blocks:
            lines = block.strip().split('\n')
            if len(lines) >= 2:
                if lines[0].strip().isdigit():
                    lines = lines[1:]
                if len(lines) >= 2:
                    ts_line = lines[0].replace(',', '.')
                    vtt_content += ts_line + '\n'
                    for text_line in lines[1:]:
                        vtt_content += text_line + '\n'
                    vtt_content += '\n'

        with open(vtt_path, 'w', encoding='utf-8') as f:
            f.write(vtt_content)
        return True
    except Exception as e:
        print(f"Convert error: {e}")
        return False


def download_subtitles(imdb_id, title, languages, output_dir):
    """Download subtitles from available sources"""
    os.makedirs(output_dir, exist_ok=True)

    print(f"Searching subtitles for IMDB: {imdb_id}, languages: {languages}")

    # Try opensubtitlescom
    results = search_opensubtitlescom(imdb_id, languages)
    print(f"Found {len(results)} results from opensubtitlescom")

    downloaded = []

    for result in results[:5]:
        attrs = result.get('attributes', {})
        files = attrs.get('files', [])

        if not files:
            continue

        file_info = files[0]
        file_id = file_info.get('file_id')
        file_name = attrs.get('file_name', '')
        language = attrs.get('language', '')

        print(f"Processing: {file_name} ({language})")

        # Try download via opensubtitlescom download endpoint
        download_url = f"https://api.opensubtitles.com/api/v1/download"

        headers = {
            "Api-Key": OPENSUBTITLES_API_KEY,
            "User-Agent": OPENSUBTITLES_USER_AGENT,
        }

        data = {"file_id": file_id}

        try:
            response = requests.post(download_url, headers=headers, json=data, timeout=60)
            print(f"Download response: {response.status_code}")

            if response.status_code == 200:
                result_data = response.json()

                # Check for direct content
                if 'content' in result_data:
                    srt_path = os.path.join(output_dir, f"{language}.srt")
                    vtt_path = os.path.join(output_dir, f"{language}.vtt")

                    # Write content
                    with open(srt_path, 'w', encoding='utf-8', errors='replace') as f:
                        f.write(result_data['content'])

                    if convert_srt_to_vtt(srt_path, vtt_path):
                        downloaded.append(vtt_path)
                        os.remove(srt_path)
                        print(f"Saved: {vtt_path}")
                        return downloaded
        except Exception as e:
            print(f"Download error: {e}")

        time.sleep(1)

    return downloaded


def main():
    parser = argparse.ArgumentParser(description='Download subtitles from multiple sources')
    parser.add_argument('--imdb-id', required=True, help='IMDB ID')
    parser.add_argument('--title', required=True, help='Video title')
    parser.add_argument('--languages', required=True, help='Comma-separated languages')
    parser.add_argument('--output-dir', required=True, help='Output directory')

    args = parser.parse_args()

    languages = args.languages.split(',')
    print(f"Downloading: {args.title} ({args.imdb_id})")
    print(f"Languages: {languages}")

    saved = download_subtitles(args.imdb_id, args.title, languages, args.output_dir)

    print(f"\nSaved files: {saved}")
    return 0 if saved else 1


if __name__ == '__main__':
    sys.exit(main())