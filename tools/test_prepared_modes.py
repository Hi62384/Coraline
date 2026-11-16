"""Launcher-mode wiring only; actual restarted-world acceptance is separate."""
import ast
from pathlib import Path
import unittest

class PreparedModes(unittest.TestCase):
    def test_both_post_escape_modes_create_the_disposable_quick_play_world(self):
        tree=ast.parse((Path(__file__).with_name('run_prepared_native.py')).read_text(encoding='utf8'))
        setup=[n for n in ast.walk(tree) if isinstance(n,ast.If) and 'args.tycoon_phase_one' in ast.unparse(n.test) and 'args.first_escape' in ast.unparse(n.test) and any(isinstance(v,ast.Constant) and v.value=='saves/WybieVisitQA' for child in n.body for v in ast.walk(child))]
        self.assertEqual(len(setup),1)
        condition=ast.unparse(setup[0].test)
        self.assertIn('args.missing_parents_tea',condition)
        self.assertIn('args.missing_parents_cat',condition)
        constants=[n.value for child in setup[0].body for n in ast.walk(child) if isinstance(n,ast.Constant) and isinstance(n.value,str)]
        self.assertIn('saves/WybieVisitQA',constants)

    def test_tycoon_resume_switch_is_not_inside_first_escape_mode(self):
        tree=ast.parse((Path(__file__).with_name('run_prepared_native.py')).read_text(encoding='utf8'))
        flag=' -Dcoraline.tycoonPhaseTwoResume=true'
        assignments=[n for n in ast.walk(tree) if isinstance(n,ast.AugAssign) and isinstance(n.value,ast.Constant) and n.value.value==flag]
        self.assertEqual(len(assignments),1)
        guards=[n for n in tree.body if isinstance(n,ast.If) and ast.unparse(n.test)=='args.resume_tycoon_two_report']
        self.assertTrue(any(assignments[0] in n.body for n in guards),'Resume property must be set for standalone Phase2, not only inside First Escape')

if __name__=='__main__':unittest.main()
