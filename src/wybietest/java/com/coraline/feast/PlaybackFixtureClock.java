package com.coraline.feast;

/** Deterministic server-controller fixture only. Never packaged or audible acceptance. */
final class PlaybackFixtureClock {
    @SuppressWarnings("unchecked") static void elapseOriginal(VisitData.Scene scene){
        var state=ShowPlayback.data(scene);if(!state.getBoolean("Started"))throw new AssertionError("Fixture must first exercise the actual start signal");
        try{var field=ShowPlayback.class.getDeclaredField("AUDIBLE_START");field.setAccessible(true);var clock=(java.util.Map<java.util.UUID,Long>)field.get(null);var nonce=state.getUUID("Nonce");if(!clock.containsKey(nonce))throw new AssertionError("No original recording start clock");clock.put(nonce,System.nanoTime()-state.getInt("Duration")*50_000_000L-1_000_000L);}catch(ReflectiveOperationException e){throw new AssertionError("Cannot advance isolated controller fixture clock",e);}
    }
    private PlaybackFixtureClock(){}
}
