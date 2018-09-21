package com.codenjoy.dojo.services;

/*-
 * #%L
 * Codenjoy - it's a dojo-like platform from developers to developers.
 * %%
 * Copyright (C) 2016 Codenjoy
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */


import com.codenjoy.dojo.transport.ws.PlayerTransport;

import java.io.IOException;

public class PlayerControllerV2Impl implements PlayerController<BoardGameStateV2, Joystick> {

    private PlayerTransport transport;

    @Override
    public void requestControlToAll(BoardGameStateV2 board) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void requestControl(Player player, BoardGameStateV2 boardState) throws IOException {
        transport.sendState(player.getName(), boardState);
    }

    @Override
    public void registerPlayerTransport(Player player, Joystick joystick) {
        transport.registerPlayerEndpoint(player.getName(),
                new PlayerResponseHandlerImpl(player, joystick));
    }

    @Override
    public void unregisterPlayerTransport(Player player) {
        transport.unregisterPlayerEndpoint(player.getName());
    }

    public void setTransport(PlayerTransport transport) {
        this.transport = transport;
        transport.setDefaultFilter(data -> data.toString());
    }
}
