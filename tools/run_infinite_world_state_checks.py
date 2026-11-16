"""Independent seed/unlock NBT checks only; shares unchanged Java/RAM guards."""
from run_ghost_escape_state_checks import main
import sys

if __name__=='__main__':
    if len(sys.argv)>2 and sys.argv[2:] not in (['--lifecycle-only'],['--ore-only']):raise ValueError('Only one focused component selection is supported')
    raise SystemExit(main(check_source='InfiniteWorldStateCheck.java',class_name='com.coraline.postgame.InfiniteWorldStateCheck',marker='INFINITE_WORLD_NBT_OK checks=',report_group='infinite-world-state-checks',label='Infinite world lifecycle',program_args=tuple(sys.argv[2:])))
