"""Actual Breach arithmetic/NBT only; no game, fuel inventory or boss claim."""
from run_ghost_escape_state_checks import main

if __name__ == '__main__':
    raise SystemExit(main(check_source='BreachStateCheck.java',
                         class_name='com.coraline.postgame.BreachStateCheck',
                         marker='BREACH_STATE_NBT_OK checks=',
                         report_group='breach-state-checks', label='Shared Breach state'))
