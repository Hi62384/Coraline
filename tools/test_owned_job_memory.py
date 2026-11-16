import unittest
from owned_job_memory import aggregate


class OwnedJobMemoryTests(unittest.TestCase):
    def test_real_interpreter_is_not_mistaken_for_thin_launcher(self):
        small={'peak_working_set_bytes':3,'working_set_bytes':2,'peak_pagefile_bytes':1,'private_bytes':1}
        model={'peak_working_set_bytes':2500,'working_set_bytes':2300,'peak_pagefile_bytes':4000,'private_bytes':3500}
        result=aggregate([small,model])
        self.assertEqual(result,{'peak_working_set_bytes':2503,'working_set_bytes':2302,'peak_pagefile_bytes':4001,'private_bytes':3501})

    def test_missing_job_members_are_not_reported_as_zero_memory(self):
        with self.assertRaisesRegex(RuntimeError,'No readable'):aggregate([])


if __name__=='__main__':unittest.main()
