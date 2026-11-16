"""Detached exact-PCM comparison, using the unchanged original baseline checker."""
import check_early_reference_voice as comparison
import generate_semantic_reference_voice as candidate
from semantic_reference_retirement import finish


def main():
    original=comparison.candidate;comparison.candidate=candidate
    # The baseline checker accesses this original generator interface while it
    # redirects only its output into a fresh, detached comparison directory.
    candidate.ROOT=comparison.ROOT;candidate.speech=original.speech
    try:comparison.main()
    finally:comparison.candidate=original


if __name__=='__main__':finish(main,__file__)
