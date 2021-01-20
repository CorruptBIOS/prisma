package com.avairebot.contracts.cache;

import net.dv8tion.jda.api.entities.Message;

import java.time.OffsetDateTime;

public class CachedMessage
{
    private final OffsetDateTime timeCreated;
    private final String contentRaw;
    private final CachedUser author;
    private final String channelId;
    private final long id;
    private final String attachment;

    public CachedMessage(Message message)
    {
        this.timeCreated = message.getTimeCreated();
        this.contentRaw = message.getContentRaw();
        this.author = new CachedUser(message.getAuthor());
        this.channelId = message.getChannel().getId();
        this.id = message.getIdLong();
        this.attachment = message.getAttachments().size() == 1 ? message.getAttachments().get(0).getUrl() : null;
    }

    public OffsetDateTime getTimeCreated()
    {
        return timeCreated;
    }

    public String getContentRaw()
    {
        return contentRaw;
    }

    public CachedUser getAuthor()
    {
        return author;
    }

    public String getChannelId()
    {
        return channelId;
    }

    public long getIdLong()
    {
        return id;
    }

    public String getAttachment() {
        return attachment;
    }
}