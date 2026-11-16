"""Exercise the caption approval gate without loading a model or writing resources."""
import ast, hashlib, json, unittest
from pathlib import Path

class OriginalTranscriptGateTests(unittest.TestCase):
    def setUp(self):
        source=ast.parse((Path(__file__).parent/'build_story_subtitles.py').read_text())
        functions=[n for n in source.body if isinstance(n,ast.FunctionDef) and n.name in ('transcript_hash','original_verified')]
        self.scope={'hashlib':hashlib,'json':json,'ORIGINALS':{'track':{'source_sha256':'source'}},'original_proof':{}}
        exec(compile(ast.Module(body=functions,type_ignores=[]),'caption_gate','exec'),self.scope)
        self.segments=[{'start':0,'end':1,'text':'Source matched text.'}]
        self.proof={'source_sha256':'source','transcript_sha256':self.scope['transcript_hash'](self.segments),'quality_warnings':[]}
        self.scope['original_proof']['track']=self.proof
    def verified(self):return self.scope['original_verified']('track',self.segments)
    def test_confident_asr_alone_is_not_approval(self):self.assertFalse(self.verified())
    def test_pending_draft_is_not_publishable(self):
        self.proof['review_status']='pending';self.assertFalse(self.verified())
    def test_approved_matching_transcript_is_publishable(self):
        self.proof['review_status']='approved';self.assertTrue(self.verified())
    def test_approval_cannot_override_uncertainty(self):
        self.proof.update(review_status='approved',quality_warnings=['uncertain_segment_0']);self.assertFalse(self.verified())
    def test_approval_cannot_override_changed_source_or_text(self):
        self.proof.update(review_status='approved',source_sha256='changed');self.assertFalse(self.verified())
        self.proof['source_sha256']='source';self.segments[0]['text']='Different';self.assertFalse(self.verified())

if __name__=='__main__':unittest.main()
