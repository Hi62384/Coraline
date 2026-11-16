"""Real binding receipt/NBT assertions only, without Minecraft bootstrap."""
from run_ghost_escape_state_checks import main

if __name__ == '__main__':
    raise SystemExit(main(check_source='SiteBindingStateCheck.java',
                         class_name='com.coraline.postgame.SiteBindingStateCheck',
                         marker='SITE_BINDING_NBT_OK checks=',
                         report_group='site-binding-state-checks', label='Site binding state'))
