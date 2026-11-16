"""Real assignment/NBT components only; no game or spawning acceptance."""
from run_ghost_escape_state_checks import main

if __name__ == '__main__':
    raise SystemExit(main(check_source='SiteGuardStateCheck.java',
                         class_name='com.coraline.postgame.SiteGuardStateCheck',
                         marker='SITE_GUARD_STATE_NBT_OK checks=',
                         report_group='site-guard-state-checks', label='Site guard state'))
