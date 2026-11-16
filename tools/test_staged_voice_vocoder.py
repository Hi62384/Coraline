import hashlib,json,tempfile,unittest
from pathlib import Path
from staged_voice_vocoder import COMPONENT_SOURCES,checkpoint_pins,verify_proof

class VocoderProofTests(unittest.TestCase):
    def test_component_proof_requires_exact_values_rng_sources_and_checkpoint(self):
        with tempfile.TemporaryDirectory() as directory:
            root=Path(directory);(root/'tools').mkdir()
            for name in COMPONENT_SOURCES:(root/'tools'/name).write_text(name)
            checkpoint=root/'.voice-tools/index-tts/checkpoints_2/hf_cache/bigvgan';checkpoint.mkdir(parents=True)
            for name in ('config.json','bigvgan_generator.pt'):(checkpoint/name).write_bytes(name.encode())
            folder=root/'build-character-cast/vocoder-staging-checks';folder.mkdir(parents=True);path=folder/'proof.json'
            proof=dict(status='passed',original_gpu_forward=True,original_precision=True,all_values_verified=True,file_backed_cpu_weights=True,exact_output=True,exact_cpu_cuda_rng=True,certifies_speech_generation=False,activations=2,verified_bytes=32,verified_tensors=2,mapped_weight_bytes=16,checkpoint_sha256=checkpoint_pins(root),source_sha256={name:hashlib.sha256((root/'tools'/name).read_bytes()).hexdigest() for name in COMPONENT_SOURCES})
            path.write_text(json.dumps(proof));verify_proof(root,path,True)
            for key,value in [('exact_output',False),('exact_cpu_cuda_rng',False),('activations',1),('mapped_weight_bytes',0),('original_precision',False),('certifies_speech_generation',True)]:
                path.write_text(json.dumps(dict(proof,**{key:value})))
                with self.subTest(key=key),self.assertRaises(ValueError):verify_proof(root,path,True)
            path.write_text(json.dumps(proof));(checkpoint/'bigvgan_generator.pt').write_bytes(b'changed')
            with self.assertRaisesRegex(ValueError,'checkpoint changed'):verify_proof(root,path,True)
            with self.assertRaisesRegex(ValueError,'Unexpected vocoder proof'):verify_proof(root,path,False)

if __name__=='__main__':unittest.main()
