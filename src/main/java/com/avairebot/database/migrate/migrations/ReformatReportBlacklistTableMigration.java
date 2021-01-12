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

package com.avairebot.database.migrate.migrations;

import com.avairebot.Constants;
import com.avairebot.contracts.database.migrations.Migration;
import com.avairebot.database.connections.SQLite;
import com.avairebot.database.schema.Schema;

import java.sql.SQLException;

public class ReformatReportBlacklistTableMigration implements Migration {

    @Override
    public String created_at() {
        return "Mon, Jan 11, 2021 12:02 PM";
    }

    @Override
    public boolean up(Schema schema) throws SQLException {
        if (hasColumn(schema, "type")) {
            return true;
        }

        if (schema.getDbm().getConnection() instanceof SQLite) {
            return true;
        }

        schema.getDbm().queryUpdate(String.format(
            "ALTER TABLE `%s` DROP `id`;",
            Constants.REPORT_BLACKLIST_TABLE_NAME
        ));

        schema.getDbm().queryUpdate(String.format(
            "ALTER TABLE `%s` DROP `user_id`;",
            Constants.REPORT_BLACKLIST_TABLE_NAME
        ));

        schema.getDbm().queryUpdate(String.format(
            "ALTER TABLE `%s` ADD `id` VARCHAR(128) NULL;",
            Constants.REPORT_BLACKLIST_TABLE_NAME
        ));

        schema.getDbm().queryUpdate(String.format(
            "ALTER TABLE `%s` ADD `type` INT(2) NOT NULL DEFAULT '0';",
            Constants.REPORT_BLACKLIST_TABLE_NAME
        ));

        return true;
    }

    @Override
    public boolean down(Schema schema) throws SQLException {
        if (!hasColumn(schema, "type")) {
            return true;
        }

        if (schema.getDbm().getConnection() instanceof SQLite) {
            return true;
        }

        schema.getDbm().queryUpdate(String.format(
            "ALTER TABLE `%s` DROP `id`;",
            Constants.REPORT_BLACKLIST_TABLE_NAME
        ));

        schema.getDbm().queryUpdate(String.format(
            "ALTER TABLE `%s` DROP `type`;",
            Constants.REPORT_BLACKLIST_TABLE_NAME
        ));

        schema.getDbm().queryUpdate(String.format(
            "ALTER TABLE `%s` ADD `id` INT NOT NULL AUTO_INCREMENT;",
            Constants.REPORT_BLACKLIST_TABLE_NAME
        ));

        schema.getDbm().queryUpdate(String.format(
            "ALTER TABLE `%s` ADD `user_id` BIGINT UNSIGNED NOT NULL;",
            Constants.REPORT_BLACKLIST_TABLE_NAME
        ));

        return false;
    }

    private boolean hasColumn(Schema schema, String name) throws SQLException {
        return schema.hasColumn(Constants.REPORT_BLACKLIST_TABLE_NAME, name);
    }
}