import ctypes
import types
import unittest
from unittest.mock import patch
import check_numbered_candidate as monitor


class MonitorCounterLifetimeTests(unittest.TestCase):
    def test_repeated_samples_do_not_grow_ctypes_pointer_type_cache(self):
        def query(handle,pointer,size):
            row=pointer._obj
            row.peak_working_set=123;row.working_set=100;row.peak_pagefile=200;row.private=150
            return True
        api=types.SimpleNamespace(GetProcessMemoryInfo=query)
        with patch.object(ctypes,'WinDLL',return_value=api,create=True):
            process=types.SimpleNamespace(_handle=1)
            sample=monitor.process_memory(process)
            baseline=len(ctypes._pointer_type_cache)
            for _ in range(256):self.assertEqual(monitor.process_memory(process),sample)
            self.assertEqual(len(ctypes._pointer_type_cache),baseline,
                'Each memory sample permanently retains another ctypes Structure/pointer class')


if __name__=='__main__':unittest.main()
