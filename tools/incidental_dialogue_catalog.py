"""Finite, already-authored parent speech missed by the event voice pools.

UI instructions/options, narration, unnamed guests and silent Other Wybie are
not new parent dialogue. Text is copied exactly from its named gameplay owner.
This module does not synthesize, transcribe, or export any recording.
"""
import hashlib
import json
from pathlib import Path

MANIFESTS = ['tools/incidental_mother_voice_manifest.json',
             'tools/incidental_father_voice_manifest.json']

MOTHER = {
    'AcceptedButtonEyes.java': [
        "Come along, dear. It's time for your new eyes.",
        "There we are, dear. All finished. Come and meet me upstairs.",
        "Those buttons weren't a gift, dear. They were how I claimed your soul.",
        "You aren't a living child to me any longer. I will feed on what remains of your life.",
        "And now there's nothing left for you to do. I think I'll keep what's left of you.",
    ],
    'DinnerActivities.java': [
        "There. Now everyone has a drink. What a thoughtful host you're becoming.",
        "Hold it steady, sweetheart. Now click each empty glass.",
        "That's our cider jug. We'll top up everyone's glasses after the first course.",
        "The bowl smells of warm honey and summer flowers. What do you think I put in it?",
        "A sharp, fruity scent rises from the bowl, with little purple berry stains at the rim.",
        "The sauce smells of rich cocoa. There are tiny chocolate shavings on the spoon.",
        "Save some room! We'll serve dessert after our little recipe game.",
        "A slice of berry cake for me, please.",
        "Thank you, sweetheart. Don't forget your Other Father.",
        "All tidied away. Stay and chat, or tell us when you're ready to finish.",
        "How is the first bite? Try the other things on your plate, too.",
        "One more taste. Every dish has a little story behind it.",
        "Is there anything else you would like, sweetheart?",
        "You've smelled the sauce. Which ingredient did you recognise?",
        "Inspect the little recipe bowl first. A good cook uses their nose as well as their eyes.",
        "Honey for warmth, berries for sharpness, chocolate for a treat. You'll find a recipe bowl on the table when it's time to guess.",
        "What shall we raise our glasses to?",
        "Perfect? Only because I pay attention to what everyone wants. You do tell me what you want, don't you?",
        "Let's enjoy the evening we have first. There is always something else waiting for us.",
        "Exactly right! A fine nose. Now let's get those desserts served.",
        "You found the secret ingredient. Berry cake for me and cookies for your Other Father, please.",
        "Not quite. Smell the recipe bowl again and look for the clue before trying another ingredient.",
        "Taste as you go, and never stop paying attention. The smallest ingredient can change everything.",
        "Of course. A little help can make a meal feel like something we've made together.",
        "To a lovely evening. Just your plate left to clear, sweetheart.",
        "To taking very good care of one another. I'll drink to that.",
        "To adventures. Though do remember to come back for supper.",
        "Home is where you're wanted, sweetheart. Remember that.",
    ],
    'MotherSearch.java': [
        "Some doors are closed for their own good. Leave them closed.",
        "There are things here too fragile for curious hands.",
        "You have been watching everything so carefully. So have I.",
        "Perhaps my presents have spoiled you.",
        "Is there something missing from what I have given you?",
        "You needn't concern yourself with every locked door.",
    ],
    'HouseParty.java': ["A toast to our guest of honour! Come join the neighbours, darling."],
    'PartyCelebration.java': ["Our guest of honour! Take your place by the cake, make a wish, and blow out the candles."],
    'SpecialDelivery.java': [
        "We'll do something else while the delivery route is prepared.",
        "Follow the path, deliver the package, and come back to me, sweetheart.",
    ],
    'VisitEvents.java': ["Getting bored already, sweetheart? Shall I find something else for us to do?"],
}

FATHER = {
    'DinnerActivities.java': [
        "That's mine filled. Your Other Mother's glass is still empty.",
        "One slice of berry cake. Take that over to your Other Mother's plate.",
        "Those cookies are for me! Click my plate to serve them.",
        "Could I have the cookies, kiddo?",
        "Perfect! You remembered both orders. Now, how about a toast?",
        "Thanks, kiddo! Could you top up our cider next? The jug's in the middle.",
        "The other dish, please. I've highlighted the one I meant.",
        "Three tastes! Which was your favourite? You can tell us at the table. Could you pass me a dish first?",
        "What would you like to do?",
        "Dinner's better when we have something to talk about, kiddo.",
        "I started with a little tune and let it wander. Sometimes the tune seems to know the house better than I do.",
        "You pick the rhythm, I'll find the notes. Tap your spoon: slow, quick, quick. There! That's our dinner song.",
        "The flowers lean toward anyone who listens. If you visit, look for the little paths between the tallest stems.",
        "What shall we raise our glasses to?",
        "The roast was lovely, but I always save a little room for cookies. What about you?",
        "She likes everything just so. I try to leave a little room for surprises in the music.",
        "That's it! A bright little chorus. We ought to remember this one.",
        "A quiet tune for a house full of secrets. You have an ear for atmosphere, kiddo.",
        "I do what I can. A little patience, a little water, and knowing when to leave things alone.",
        "I sometimes wonder that myself. Best not to tell a flower anything you wouldn't sing aloud.",
        "That's a toast worth keeping, kiddo. Nobody should have to do everything alone.",
        "To all the things we haven't discovered yet!",
        "To home. And to finding your own way there.",
    ],
    'FamilyDinner.java': [
        "Thank you, kiddo!",
        "Music makes a house feel like home, doesn't it? I keep finding new melodies here.",
        "The garden has a way of surprising me too. Even the flowers seem to be listening.",
        "Fair enough, kiddo. We all need a quiet moment sometimes.",
        "Of course. Take your time and enjoy it.",
        "Thank you! Right-click the highlighted dish when you're ready to pass it.",
        "What have you enjoyed most about the house?",
        "Could you give me a hand with the table?",
        "So, what have you been getting up to today?",
    ],
    'PartyCelebration.java': ["Make a wish, kiddo. Ready? Click the cake again and blow!"],
    'MiceCircusActivity.java': ["A show souvenir for you: two Perfect Ingots!"],
}


def catalogue():
    records = {}
    for who, groups in [('mother', MOTHER), ('father', FATHER)]:
        speaker = 'Other ' + who.title()
        for owner, texts in groups.items():
            for text in texts:
                key = who + '_' + hashlib.sha256(text.encode('utf-8')).hexdigest()[:16]
                if key in records:
                    raise ValueError('Duplicate incidental parent line: ' + key)
                records[key] = {'speaker': speaker, 'text': text, 'owner': owner}
    return records


def recordings(root):
    """Use the same strict receipt convention as the established legacy importer."""
    from voice_recording_proof import legacy_recordings
    return legacy_recordings(root, MANIFESTS)


def build(root):
    root = Path(root)
    runtime = []
    for who, filename in zip(('mother', 'father'), MANIFESTS):
        base = json.loads((root / ('tools/other_mother_welcome.json' if who == 'mother'
                                  else 'tools/other_father_activity_intros.json')).read_text(encoding='utf-8'))
        manifest = {key: base[key] for key in ('reference', 'reference_sha256')}
        manifest.update(output=f'voicelines/other{who}/incidental_dialogue_v1',
                        prefix='line', resource_folder='incidental/' + who,
                        notice='src/main/resources/META-INF/INCIDENTAL_PARENT_AUDIO_NOTICE.txt',
                        max_seconds=120, states={})
        for key, row in catalogue().items():
            if not key.startswith(who + '_'):
                continue
            event = 'other_mother_incidental_' + key if who == 'mother' else 'father_incidental_' + key.removeprefix('father_')
            cool = row['owner'] == 'MotherSearch.java' or (row['owner']=='AcceptedButtonEyes.java' and row['text'] not in MOTHER['AcceptedButtonEyes.java'][:2])
            manifest['states'][key] = dict(
                lines=[row['text']], sound_name=event,
                direction=('Quiet, watchful and controlled; a pointed warning, not shouted.' if cool
                           else 'Natural, warm and conversational; keep every authored word, with clear unhurried delivery.'),
                emotion=[0 if cool else .35, .12 if cool else 0, 0, 0, 0, 0, 0, .75 if cool else .55],
                temperature=.72)
            runtime.append(dict(speaker=row['speaker'], text=row['text'], sound=event))
        (root / filename).write_text(json.dumps(manifest, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')
    output = root / 'src/main/resources/assets/coraline/incidental_dialogue.json'
    output.write_text(json.dumps(runtime, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')
    return {'mother': len([r for r in runtime if r['speaker'] == 'Other Mother']),
            'father': len([r for r in runtime if r['speaker'] == 'Other Father']), 'total': len(runtime)}


if __name__ == '__main__':
    print(json.dumps(build(Path(__file__).resolve().parents[1])))
