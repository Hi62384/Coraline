import json
from pathlib import Path
import tempfile
from types import SimpleNamespace
import unittest
import numpy as np
import fixed_reference_cache as cache


class Tensor:
    def __init__(self,array):self.array=array;self.dtype=array.dtype;self.device=None
    def to(self,device):self.device=device;return self


class FixedReferenceCacheTests(unittest.TestCase):
    def artifact(self,root):
        context={'schema':1,'reference_sha256':'a'*64,'files':{},'contract':{}}
        arrays={name:np.arange(12,dtype=np.float32).reshape(1,3,4)+i for i,name in enumerate(cache.NAMES)}
        folder=cache.cache_base(root,context)/'attempt';folder.mkdir(parents=True)
        np.savez_compressed(folder/'conditioning.npz',**arrays)
        (folder/'metadata.json').write_text(json.dumps({'context':context,'tensors':cache.fingerprints(arrays),'cpu_reference_rng_draws':48}))
        proof={'status':'passed','original_reference_path':True,'speech_generated':False,
               'random_states_preserved':True,'cpu_rng_replay_verified':True,'reference_seed_independent_tensors':True,
               'reference_speech_models_unchanged':True,'exact_tensor_roundtrip':True,'context_sha256':cache.json_digest(context),
               'metadata_sha256':cache.digest(folder/'metadata.json'),'payload_sha256':cache.digest(folder/'conditioning.npz')}
        (folder/'verified.json').write_text(json.dumps(proof))
        return context,arrays,folder

    def test_lossless_arrays_and_exact_reference_restore(self):
        with tempfile.TemporaryDirectory() as directory:
            root=Path(directory);context,original,folder=self.artifact(root)
            loaded=cache.load(root,context,np,required=True)
            for name in cache.NAMES:self.assertTrue(np.array_equal(original[name],loaded[0][name]))
            engine=SimpleNamespace(device='cuda:0',semantic_model=object())
            cache.restore(engine,Path('fixed-original.wav'),loaded,SimpleNamespace(from_numpy=Tensor))
            self.assertIsNone(engine.semantic_model);self.assertTrue(engine._coraline_reference_retired)
            self.assertEqual(engine.cache_spk_audio_prompt,engine.cache_emo_audio_prompt)
            for name in cache.NAMES:
                tensor=getattr(engine,name);self.assertEqual(tensor.device,'cuda:0')
                self.assertEqual(tensor.dtype,original[name].dtype);self.assertTrue(np.array_equal(tensor.array,original[name]))

    def test_changed_archive_and_unproved_cache_fail_closed(self):
        with tempfile.TemporaryDirectory() as directory:
            root=Path(directory);context,_,folder=self.artifact(root)
            proof_path=folder/'verified.json';proof=json.loads(proof_path.read_text())
            for field in ('original_reference_path','random_states_preserved','cpu_rng_replay_verified','reference_seed_independent_tensors','reference_speech_models_unchanged','exact_tensor_roundtrip'):
                proof_path.write_text(json.dumps(dict(proof,**{field:False})))
                with self.subTest(field=field),self.assertRaises(ValueError):cache.load(root,context,np,required=True)
            proof_path.write_text(json.dumps(proof))
            with (folder/'conditioning.npz').open('ab') as stream:stream.write(b'changed')
            with self.assertRaisesRegex(ValueError,'Changed reference'):cache.load(root,context,np,required=True)

    def test_context_mismatch_and_missing_cache_never_fall_back(self):
        with tempfile.TemporaryDirectory() as directory:
            root=Path(directory);context,_,folder=self.artifact(root)
            with self.assertRaisesRegex(ValueError,'context changed'):
                cache.verify_artifact(folder/'verified.json',dict(context,reference_sha256='b'*64))
            absent=dict(context,reference_sha256='b'*64)
            self.assertIsNone(cache.load(root,absent,np))
            with self.assertRaisesRegex(ValueError,'Prepare the exact'):cache.load(root,absent,np,required=True)

    def test_omitted_reference_or_speech_models_cannot_execute(self):
        called=[]
        inference=SimpleNamespace(load_checkpoint=lambda model,*a,**k:called.append(model),
                                  UnifiedVoice=None,bigvgan=SimpleNamespace(BigVGAN=None))
        cache.reference_only_constructor(inference)
        model=inference.UnifiedVoice();inference.load_checkpoint(model,'unused')
        self.assertEqual(called,[])
        for operation in (lambda:model.inference_speech(),lambda:model(None),
                          lambda:inference.bigvgan.BigVGAN.from_pretrained('unused')(None)):
            with self.assertRaises(RuntimeError):operation()
        cache.skip_reference_encoder(inference)
        with self.assertRaises(RuntimeError):inference.Wav2Vec2BertModel.from_pretrained('unused')(None)
        inference.load_checkpoint('real-reference-model','required')
        self.assertEqual(called,['real-reference-model'])

    def test_original_cpu_random_draws_replay_once_only(self):
        calls=[];torch=SimpleNamespace(float32='float32',rand=lambda *a,**k:calls.append((a,k)))
        engine=SimpleNamespace(_coraline_reference_rng_pending=48)
        cache.replay_reference_rng(engine,torch);cache.replay_reference_rng(engine,torch)
        self.assertEqual(len(calls),48)
        self.assertTrue(all(a==([],) and k=={'device':'cpu','dtype':'float32'} for a,k in calls))
        engine._coraline_reference_rng_pending=513
        with self.assertRaises(ValueError):cache.replay_reference_rng(engine,torch)

    def test_reference_model_and_processing_inputs_bind_context(self):
        with tempfile.TemporaryDirectory() as directory:
            root=Path(directory);engine=root/'.voice-tools/index-tts';model=engine/'checkpoints_2'
            inputs=['tools/fixed_reference_cache.py','tools/low_memory_voice_loader.py','tools/generate_other_mother_welcome.py',
                    '.voice-tools/index-tts/indextts/infer_v2.py']
            for name in inputs:
                target=root/name;target.parent.mkdir(parents=True,exist_ok=True);target.write_text('processing')
            model.mkdir(parents=True,exist_ok=True)
            (model/'config.yaml').write_text('s2mel_checkpoint: s2mel.pth\nw2v_stat: stats.pt\n')
            for relative in ('s2mel.pth','stats.pt','hf_cache/w2v-bert-2.0/model.safetensors',
                             'hf_cache/w2v-bert-2.0/config.json','hf_cache/maskgct/semantic_codec/model.safetensors',
                             'hf_cache/campplus/campplus_cn_common.bin'):
                target=model/relative;target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(b'weights')
            for package in ('torch','torchaudio','numpy','transformers','safetensors','soundfile','librosa','omegaconf'):
                target=engine/'.venv/Lib/site-packages'/(package+'-1.dist-info/METADATA');target.parent.mkdir(parents=True);target.write_text('Version: 1')
            source=engine/'.venv/Lib/site-packages/transformers/models/wav2vec2_bert/modeling_wav2vec2_bert.py'
            source.parent.mkdir(parents=True);source.write_text('original reference encoder')
            reference=root/'reference.wav';reference.write_bytes(b'original')
            first=cache.build_context(root,reference,model)
            (model/'hf_cache/w2v-bert-2.0/model.safetensors').write_bytes(b'changed weights')
            second=cache.build_context(root,reference,model)
            self.assertNotEqual(cache.json_digest(first),cache.json_digest(second))
            reference.write_bytes(b'changed reference')
            self.assertNotEqual(second['reference_sha256'],cache.build_context(root,reference,model)['reference_sha256'])


if __name__=='__main__':unittest.main()
