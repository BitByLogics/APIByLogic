package net.justugh.japi.database.redis.listener;

public abstract class RedisMessageListener {

    private final String channelName;

    public RedisMessageListener(String channelName) {
        this.channelName = channelName;
    }

    public String getChannelName() {
        return channelName;
    }

    /**
     * Called when an incoming Redis message matches
     * the specified channel name.
     *
     * @param message The message associated with the incoming channel message.
     */
    public abstract void onReceive(ListenerComponent message);
}
