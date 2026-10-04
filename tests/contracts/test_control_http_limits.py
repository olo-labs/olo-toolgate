# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
"""Regression for Control CI uploads interrupted by an early HTTP 413."""
import http.client
import http.server
import threading
import unittest
from unittest.mock import MagicMock, patch

from tools.control.check import oversized_request


class ControlHttpLimitsTests(unittest.TestCase):
    def test_rejection_is_read_before_uploading_body(self):
        connection = MagicMock()
        response = connection.getresponse.return_value
        response.status = 413
        response.read.return_value = b''
        response.headers = {'Content-Length':'0'}
        with patch('tools.control.check.http.client.HTTPConnection', return_value=connection):
            self.assertEqual(oversized_request('http://localhost/users', 'test', 2097153)[0], 413)
        connection.putheader.assert_any_call('Content-Length', '2097153')
        connection.send.assert_not_called()
        connection.getresponse.assert_called_once()
        connection.close.assert_called_once()

    def test_disconnect_without_http_response_is_not_accepted(self):
        connection = MagicMock()
        connection.getresponse.side_effect = http.client.RemoteDisconnected()
        with patch('tools.control.check.http.client.HTTPConnection', return_value=connection):
            with self.assertRaises(http.client.RemoteDisconnected):
                oversized_request('http://localhost/users', 'test', 2097153)
        connection.close.assert_called_once()

    def test_real_http_early_response_and_non_rejection(self):
        observed = []

        class Receiver(http.server.BaseHTTPRequestHandler):
            def do_POST(self):
                observed.append((self.path, self.headers['Content-Length']))
                self.send_response(413 if self.path == '/reject' else 200)
                self.send_header('Content-Length', '0')
                self.end_headers()
                self.close_connection = True

            def log_message(self, *args):
                pass

        server = http.server.ThreadingHTTPServer(('127.0.0.1', 0), Receiver)
        thread = threading.Thread(target=server.serve_forever, daemon=True)
        thread.start()
        try:
            base = f'http://127.0.0.1:{server.server_port}'
            self.assertEqual(oversized_request(base+'/reject', 'test', 2097153)[0], 413)
            self.assertEqual(oversized_request(base+'/accept', 'test', 1024)[0], 200)
            self.assertEqual(observed, [('/reject', '2097153'), ('/accept', '1024')])
        finally:
            server.shutdown()
            thread.join(timeout=5)
            server.server_close()
