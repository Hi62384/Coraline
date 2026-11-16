"""Only real NBT/metadata components; no game launch or gameplay acceptance."""
from run_ghost_escape_state_checks import main

if __name__ == '__main__':
    raise SystemExit(main(check_source='SiteCacheReceiptStateCheck.java',
                         class_name='com.coraline.postgame.SiteCacheReceiptStateCheck',
                         marker='SITE_CACHE_RECEIPT_NBT_OK checks=',
                         report_group='site-cache-receipt-checks', label='Site cache receipt'))
