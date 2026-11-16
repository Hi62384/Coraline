import com.coraline.secrets.PartyReportQueue;

public class PartyReportQueueCheck {
    public static void main(String[] args) {
        String[] states = {"low_low", "mid_mid", "high_low", "low_high", "high_high"};
        int checked = 0;
        for (String state : states) {
            String oldPool = "events/player_party/report/" + state;
            String newPool = "events/player_party/guest_report/" + state;
            String oldPrefix = "other_mother_" + oldPool.replace('/', '_');
            String newPrefix = "other_mother_" + newPool.replace('/', '_');
            for (int i = 1; i <= 2; i++) {
                String suffix = "_0" + i;
                var fixed = PartyReportQueue.repair(oldPool, oldPrefix + suffix, oldPool, newPool);
                if (fixed == null || !fixed.pool().equals(newPool) || !fixed.sound().equals(newPrefix + suffix))
                    throw new AssertionError("Failed legacy repair: " + state + suffix);
                if (PartyReportQueue.repair(newPool, newPrefix + suffix, oldPool, newPool) != null)
                    throw new AssertionError("Changed a correct queue");
                if (PartyReportQueue.repair("events/house_party/report/" + state, oldPrefix + suffix, oldPool, newPool) != null)
                    throw new AssertionError("Changed another pool");
                checked += 3;
            }
            if (PartyReportQueue.repair(oldPool, oldPrefix + "_03", oldPool, newPool) != null
                    || PartyReportQueue.repair(oldPool, "unrelated", oldPool, newPool) != null
                    || PartyReportQueue.repair(oldPool, "", oldPool, newPool) != null)
                throw new AssertionError("Changed an invalid or unrelated selection");
            checked += 3;
        }
        System.out.println("Party report queue: " + checked + " assertions passed");
    }
}
