/*
 * Copyright (c) 2018.
 *
 * This file is part of AvaIre.
 *
 * AvaIre is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AvaIre is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AvaIre.  If not, see <https://www.gnu.org/licenses/>.
 *
 *
 */

package com.avairebot.reportblacklist;

import com.avairebot.AvaIre;
import com.avairebot.Constants;
import com.avairebot.database.collection.Collection;
import com.avairebot.database.query.ChangeableStatement;
import com.avairebot.time.Carbon;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@SuppressWarnings("SuspiciousMethodCalls")
public class ReportBlacklist {

    private final AvaIre avaire;
    private final List<ReportBlacklistEntity> blacklist;

    /**
     * Creates a new blacklist instance.
     *
     * @param avaire The main avaire instance.
     */
    public ReportBlacklist(AvaIre avaire) {
        this.avaire = avaire;

        this.blacklist = new ReportBlacklistList();
    }

    /**
     * Checks if the author of the message, of the message was
     * sent in a guild, that the guild is on the blacklist.
     *
     * @param message The message that should be checked.
     * @return <code>True</code> if either the user, or the entire
     * server is blacklisted, <code>False</code> otherwise.
     */
    public boolean isBlacklisted(@Nonnull Message message) {
        return isBlacklisted(message.getAuthor(), message.getGuild().getIdLong());
    }

    /**
     * Checks if the given user is on the blacklist.
     *
     * @param user The user that should be checked.
     * @return <code>True</code> if the user is on the blacklist, <code>False</code> otherwise.
     */
    public boolean isBlacklisted(@Nonnull User user, long guildId) {
        if (avaire.getBotAdmins().getUserById(user.getIdLong(), true).isAdmin()) {
            return false;
        }

        ReportBlacklistEntity entity = getEntity(user.getIdLong(), Scope.USER, guildId);
        return entity != null && entity.isBlacklisted();
    }

    /**
     * Adds the given user to the blacklist with the given reason, the blacklist
     * @param reason The reason for the user being added to the blacklist.
     */
    public void addUser(@Nonnull Member member, @Nullable String reason, long guildId) {
        addIdToBlacklist(Scope.USER, member.getIdLong(), reason, guildId);
    }

    /**
     * Removes the blacklist record with the given ID.
     *
     * @param id The ID to remove from teh blacklist.
     */
    public void remove(long id, long guildId) {
        if (!blacklist.contains(id)) {
            return;
        }

        Iterator<ReportBlacklistEntity> iterator = blacklist.iterator();
        while (iterator.hasNext()) {
            ReportBlacklistEntity next = iterator.next();

            if (next.getId() == id && next.getGuildId() == guildId) {
                iterator.remove();
                break;
            }
        }

        try {
            avaire.getDatabase().newQueryBuilder(Constants.REPORT_BLACKLIST_TABLE_NAME)
                .where("id", id)
                .delete();
        } catch (SQLException e) {
            AvaIre.getLogger().error("Failed to sync blacklist with the database: " + e.getMessage(), e);
        }
    }

    /**
     * Gets the blacklist entity for the given ID.
     *
     * @param id The ID to get the blacklist entity for.
     * @return Possibly-null, the blacklist entity matching the given ID.
     */
    @Nullable
    public ReportBlacklistEntity getEntity(long id, Long guild) {
        return getEntity(id, null, guild);
    }

    /**
     * Gets the blacklist entity for the given ID, matching the given scope.
     *
     * @param id    The ID to get the blacklist entity for.
     * @param scope The scope that the blacklist entity should belong to.
     * @return Possible-null, the blacklist entity matching the given ID and scope.
     */
    @Nullable
    public ReportBlacklistEntity getEntity(long id, @Nullable Scope scope, Long guild) {
        for (ReportBlacklistEntity entity : blacklist) {
            if (entity.getId() == id && entity.getGuildId() == guild) {
                if (scope != null && scope != entity.getScope()) {
                    continue;
                }
                return entity;
            }
        }
        return null;
    }

    /**
     * Adds the ID to the blacklist with the given scope and reason.
     *
     * @param scope  The scope to register the blacklist record under.
     * @param id     The ID that should be added to the blacklist.
     * @param reason The reason that the ID was added to the blacklist.
     */
    public void addIdToBlacklist(Scope scope, final long id, final @Nullable String reason, Long guild) {
        addIdToBlacklist(scope, id, reason, null, guild);
    }

    /**
     * Adds the ID to the blacklist with the given scope, reason, and expire time.
     *
     * @param scope     The scope to register the blacklist record under.
     * @param id        The ID that should be added to the blacklist.
     * @param reason    The reason that the ID was added to the blacklist.
     * @param expiresIn The carbon time instance for when the entity should expire.
     */
    public void addIdToBlacklist(Scope scope, final long id, final @Nullable String reason, @Nullable Carbon expiresIn, Long guild) {
        ReportBlacklistEntity entity = getEntity(id, scope, guild);
        if (entity != null) {
            blacklist.remove(entity);
        }

        blacklist.add(new ReportBlacklistEntity(scope, id, reason, expiresIn, guild));

        try {
            avaire.getDatabase().newQueryBuilder(Constants.REPORT_BLACKLIST_TABLE_NAME)
                .where("id", id)
                .andWhere("guild_id", guild)
                .andWhere("type", scope.getId())
                .delete();

            avaire.getDatabase().newQueryBuilder(Constants.REPORT_BLACKLIST_TABLE_NAME)
                .useAsync(true)
                .insert((ChangeableStatement statement) -> {
                    statement.set("id", id);
                    statement.set("type", scope.getId());
                    statement.set("expires_in", expiresIn);
                    statement.set("guild_id", guild);

                    if (expiresIn == null) {
                        statement.set("expires_in", Carbon.now().addYears(10));
                    }

                    if (reason != null) {
                        statement.set("reason", reason);
                    }
                });
        } catch (SQLException e) {
            AvaIre.getLogger().error("Failed to sync blacklist with the database: " + e.getMessage(), e);
        }
    }

    /**
     * Get the all the entities currently on the blacklist, this
     * includes both users and guilds, the type can be checked
     * through the {@link ReportBlacklistEntity#getScope() scope}.
     *
     * @return The entities currently on the blacklist.
     */
    public List<ReportBlacklistEntity> getBlacklistEntities() {
        return blacklist;
    }

    /**
     * Syncs the blacklist with the database.
     */
    public synchronized void syncBlacklistWithDatabase() {
        blacklist.clear();
        try {
            Collection collection = avaire.getDatabase().newQueryBuilder(Constants.REPORT_BLACKLIST_TABLE_NAME)
                .where("expires_in", ">", Carbon.now())
                .get();

            collection.forEach(row -> {
                String id = row.getString("id", null);
                if (id == null) {
                    return;
                }

                try {
                    long longId = Long.parseLong(id);
                    Scope scope = Scope.fromId(row.getInt("type", 0));

                    blacklist.add(new ReportBlacklistEntity(
                        scope, longId,
                        row.getString("reason"),
                        row.getTimestamp("expires_in"),
                        row.getLong("guild_id")
                    ));
                } catch (NumberFormatException ignored) {
                    // This is ignored
                }
            });
        } catch (SQLException e) {
            AvaIre.getLogger().error("Failed to sync blacklist with the database: " + e.getMessage(), e);
        }
    }

    private class ReportBlacklistList extends ArrayList<ReportBlacklistEntity> {

        public boolean contains(Object userId, Object guildId) {
            if (userId instanceof Long && guildId instanceof Long) {
                long id = (long) userId;
                long gid = (long) guildId;
                for (ReportBlacklistEntity entity : this) {
                    if (entity.getId() == id && entity.getGuildId() == gid) {
                        return true;
                    }
                }
                return false;
            }

            return super.contains(userId) || super.contains(guildId);
        }
    }
}
