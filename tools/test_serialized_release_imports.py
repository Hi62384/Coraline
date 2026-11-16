import json,tempfile,unittest
from pathlib import Path
from unittest.mock import patch
import serialized_release_imports as serial

class SerializedImportReceiptTests(unittest.TestCase):
    def test_receipt_requires_every_step_and_unchanged_inputs_outputs(self):
        graph={'format':1,'version':'test','tasks':[{'name':'first'},{'name':'second'}]}
        with tempfile.TemporaryDirectory() as temporary:
            root=Path(temporary);folder=root/'build-character-cast';folder.mkdir()
            receipt=folder/'receipt.json';proof={'status':'passed','graph_path':str(folder/'graph.json'),'graph':graph,
                'steps':[{'name':name,'status':'passed','exit_code':0} for name in ('first','second')],
                'inputs':{'master':'original'},'outputs':{'caption':'approved'}}
            def write():receipt.write_text(json.dumps(proof),encoding='utf-8')
            with patch.object(serial,'ROOT',root),patch.object(serial,'graph_at',return_value=graph):
                write()
                with patch.object(serial,'snapshot',side_effect=[proof['inputs'],proof['outputs']]):self.assertEqual(serial.verify(receipt),proof)
                with patch.object(serial,'snapshot',return_value={'master':'changed'}),self.assertRaisesRegex(RuntimeError,'inputs changed'):serial.verify(receipt)
                with patch.object(serial,'snapshot',side_effect=[proof['inputs'],{'caption':'changed'}]),self.assertRaisesRegex(RuntimeError,'outputs changed'):serial.verify(receipt)
                proof['steps'].pop();write()
                with self.assertRaisesRegex(RuntimeError,'Incomplete serial imports'):serial.verify(receipt)

if __name__=='__main__':unittest.main()
