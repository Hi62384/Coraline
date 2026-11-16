"""Original early-reference speech with its cached-only CPU encoder retired sooner."""
import generate_early_reference_voice as original
from semantic_reference_retirement import install,finish


def main():
    prior=original.EarlyRetirement;original.EarlyRetirement=install(prior)
    try:original.main()
    finally:original.EarlyRetirement=prior


if __name__=='__main__':finish(main,__file__)
