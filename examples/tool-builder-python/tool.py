# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
"""Paste into the Python editor; the fixed managed runner supplies arguments."""
def tool(arguments):
    return {'text': arguments['text'].strip()}
