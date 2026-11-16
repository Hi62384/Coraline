"""Actual NBT preference checks; no item generation or game acceptance."""
from run_ghost_escape_state_checks import main

if __name__ == '__main__':
    raise SystemExit(main(check_source='VisitGiftHistoryStateCheck.java',
                         class_name='com.coraline.feast.VisitGiftHistoryStateCheck',
                         marker='VISIT_GIFT_HISTORY_NBT_OK checks=',
                         report_group='visit-gift-history-checks', label='Visit gift history'))
