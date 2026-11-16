"""Convert TSRG2 to SRG retaining only the class-name lookup in memory."""
from pathlib import Path
import argparse
import re


def convert(source, destination):
    classes = {}
    with source.open(encoding='utf-8') as stream:
        if stream.readline().split() != ['tsrg2', 'left', 'right']:
            raise ValueError('Expected the production left/right TSRG2 mapping')
        for line in stream:
            if line.strip() and not line.startswith('\t'):
                original, mapped = line.split()
                if original in classes:
                    raise ValueError('Duplicate class mapping')
                classes[original] = mapped
    if not classes:
        raise ValueError('Empty class mapping')
    def descriptor(value):
        return re.sub(r'L([^;]+);', lambda match: 'L' + classes.get(match[1], match[1]) + ';', value)
    temporary = destination.with_suffix('.pending.srg')
    destination.parent.mkdir(parents=True, exist_ok=True)
    try:
        with temporary.open('w', encoding='utf-8', newline='\n') as output:
            for original, mapped in classes.items():
                output.write(f'CL: {original} {mapped}\n')
            for methods in (False, True):
                owner = mapped_owner = None
                with source.open(encoding='utf-8') as stream:
                    next(stream)
                    for line in stream:
                        if not line.strip() or line.startswith('\t\t'):
                            continue
                        parts = line.split()
                        if not line.startswith('\t'):
                            owner, mapped_owner = parts
                            continue
                        if owner is None or len(parts) not in (2, 3):
                            raise ValueError('Invalid member mapping')
                        is_method = len(parts) == 3 and parts[1].startswith('(')
                        if is_method != methods:
                            continue
                        if is_method:
                            name, desc, mapped = parts
                            output.write(f'MD: {owner}/{name} {desc} {mapped_owner}/{mapped} {descriptor(desc)}\n')
                        else:
                            output.write(f'FD: {owner}/{parts[0]} {mapped_owner}/{parts[-1]}\n')
        temporary.replace(destination)
    finally:
        temporary.unlink(missing_ok=True)


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('source', type=Path)
    parser.add_argument('destination', type=Path)
    args = parser.parse_args()
    convert(args.source, args.destination)
