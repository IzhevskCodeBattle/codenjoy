package com.codenjoy.dojo.bomberman.model;

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


import com.codenjoy.dojo.bomberman.services.Events;
import com.codenjoy.dojo.services.Point;
import com.codenjoy.dojo.services.PointImpl;
import com.codenjoy.dojo.services.Tickable;
import com.codenjoy.dojo.services.printer.BoardReader;
import com.codenjoy.dojo.services.settings.Parameter;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 * User: oleksandr.baglai
 * Date: 3/7/13
 * Time: 9:11 AM
 */
public class Bomberman implements Tickable, Field {

    private List<Player> players = new LinkedList<>();
    private List<Player> botPlayers = new LinkedList<>();
    private List<Player> nonBotPlayers = new LinkedList<>();

    private Walls walls;
    private Parameter<Integer> size;
    private List<Bomb> bombs;
    private List<Blast> blasts;
    private List<MeatChopper> choppers;
    private GameSettings settings;
    private List<PointImpl> destroyedWalls;
    private List<Bomb> destroyedBombs;

    public Bomberman(GameSettings settings) {
        this.settings = settings;
        bombs = new LinkedList<Bomb>();
        blasts = new LinkedList<Blast>();
        destroyedWalls = new LinkedList<PointImpl>();
        destroyedBombs = new LinkedList<Bomb>();
        size = settings.getBoardSize();
        walls = settings.getWalls(this);  // TODO как-то красивее сделать
        findChoppers();
    }

    public GameSettings getSettings() {
        return settings;
    }

    @Override
    public int size() {
        return size.getValue();
    }

    @Override
    public void tick() {
        findChoppers();
        removeBlasts();
        tactAllBombermans();
        meatChopperEatBombermans();
        walls.tick();
        meatChopperEatBombermans();
        tactAllBombs();
        cleanupChoppers();
    }

    private void findChoppers() {
        choppers = walls.subList(MeatChopper.class);
    }

    private void cleanupChoppers() {
        choppers.clear();
    }

    private void tactAllBombermans() {
        for (Player player : players) {
            player.getBomberman().apply();
        }
    }

    private void removeBlasts() {
        blasts.clear();
        for (PointImpl pt : destroyedWalls) {
            walls.destroy(pt.getX(), pt.getY());
        }
        destroyedWalls.clear();
    }

    private void wallDestroyed(Wall wall, Blast blast) {
        for (Player player : players) {
            if (blast.itsMine(player.getBomberman())) {
                if (wall instanceof MeatChopper) {
                    player.event(Events.KILL_MEAT_CHOPPER);
                } else if (wall instanceof DestroyWall) {
                    player.event(Events.KILL_DESTROY_WALL);
                }
            }
        }
    }

    private void meatChopperEatBombermans() {
        for (MeatChopper chopper : walls.subList(MeatChopper.class)) {
            for (Player player : players) {
                Hero bomberman = player.getBomberman();
                if (bomberman.isAlive() && chopper.itsMe(bomberman) && !bomberman.isBot()) {
                    player.event(Events.KILL_BOMBERMAN);
                }
            }
        }

        botPlayersEatPlayers();
//        List<Point> meatChoppers = walls.subList(MeatChopper.class);
//        List<Point> botPlayers = players.stream()
//                .filter(Player::isBot)
//                .map(Player::getBomberman)
//                .collect(Collectors.toCollection(LinkedList::new));
//        meatChoppers.addAll(botPlayers);
//
//        for (Point chopper : meatChoppers) {
//            for (Player player : players) {
//                if (player.isBot()) {
//                    continue;
//                }
//
//                Hero bomberman = player.getBomberman();
//                if (bomberman.isAlive() && chopper.itsMe(bomberman)) {
//                    player.event(Events.KILL_BOMBERMAN);
//                }
//            }
//        }
    }

    private void botPlayersEatPlayers() {
        for (Player botPlayer : botPlayers) {
            for (Player player : nonBotPlayers) {
                Hero bomberman = player.getBomberman();
                if (!bomberman.isBot() && bomberman.isAlive() && botPlayer.getBomberman().itsMe(bomberman)) {
                    player.event(Events.KILL_BOMBERMAN);
                    botPlayer.event(Events.KILL_OTHER_BOMBERMAN);
                }
            }
        }
    }

    private void tactAllBombs() {
        for (Bomb bomb : bombs) {
            bomb.tick();
        }

        for (Bomb bomb : destroyedBombs) {
            bombs.remove(bomb);

            List<Blast> blast = makeBlast(bomb);
            killAllNear(blast, bomb);
            blasts.addAll(blast);
        }
        destroyedBombs.clear();
    }

    @Override
    public List<Bomb> getBombs() {
        return bombs;
    }

    @Override
    public List<Bomb> getBombs(HeroImpl bomberman) {
        List<Bomb> result = new LinkedList<Bomb>();
        for (Bomb bomb : bombs) {
            if (bomb.itsMine(bomberman)) {
                result.add(bomb);
            }
        }
        return result;
    }

    @Override
    public List<Blast> getBlasts() {
        return blasts;
    }

    @Override
    public void drop(Bomb bomb) {
        if (!existAtPlace(bomb.getX(), bomb.getY())) {
            bombs.add(bomb);
        }
    }

    @Override
    public void removeBomb(Bomb bomb) {
        destroyedBombs.add(bomb);
    }

    private List<Blast> makeBlast(Bomb bomb) {
        List barriers = (List) walls.subList(Wall.class);
        barriers.addAll(getBombermans());

        return new BoomEngineOriginal(bomb.getOwner()).boom(barriers, size.getValue(), bomb, bomb.getPower());   // TODO move bomb inside BoomEngine
    }

    private void killAllNear(List<Blast> blasts, Bomb bomb) {
        for (Blast blast: blasts) {
            if (walls.itsMe(blast.getX(), blast.getY())) {
                destroyedWalls.add(blast);

                Wall wall = walls.get(blast.getX(), blast.getY());
                wallDestroyed(wall, blast);
            }
        }
        for (Blast blast: blasts) {
            for (Player dead : players) {
                if (dead.getBomberman().itsMe(blast)) {
                    dead.event(Events.KILL_BOMBERMAN);

                    for (Player bombOwner : players) {
                        if (dead != bombOwner && blast.itsMine(bombOwner.getBomberman())) {
                            bombOwner.event(Events.KILL_OTHER_BOMBERMAN);
                        }
                    }
                }
            }
        }
    }

    private boolean existAtPlace(int x, int y) {
        for (Bomb bomb : bombs) {
            if (bomb.getX() == x && bomb.getY() == y) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Walls getWalls() {
         return new WallsImpl(walls);
    }

    @Override
    public boolean isBarrier(Point botPos, int newX, int newY, boolean isWithMeatChopper) {
        if (botPos != null && isAnotherBomberman(botPos, newX, newY)) {
            return false;
        }
        if (botPos != null && isMeetChopper(PointImpl.pt(newX, newY))) {
            return true;
        }
        for (Hero bomberman : getBombermans()) {
            if (bomberman.itsMe(new PointImpl(newX, newY))) {
                return true;
            }
        }
        for (Bomb bomb : bombs) {
            if (bomb.itsMe(newX, newY)) {
                return true;
            }
        }
        for (Wall wall : walls) {
            if (wall instanceof MeatChopper && !isWithMeatChopper) {
                continue;
            }
            if (wall.itsMe(newX, newY)) {
                return true;
            }
        }
        return newX < 0 || newY < 0 || newX > size() - 1 || newY > size() - 1;
    }

    @Override
    public boolean isBarrier(int x, int y, boolean isWithMeatChopper) {
        for (Hero bomberman : getBombermans()) {
            if (bomberman.itsMe(new PointImpl(x, y))) {
                return true;
            }
        }
        for (Bomb bomb : bombs) {
            if (bomb.itsMe(x, y)) {
                return true;
            }
        }
        for (Wall wall : walls) {
            if (wall instanceof MeatChopper && !isWithMeatChopper) {
                continue;
            }
            if (wall.itsMe(x, y)) {
                return true;
            }
        }
        return x < 0 || y < 0 || x > size() - 1 || y > size() - 1;
    }

    public Optional<Hero> getAnotherHero(int x, int y) {
        for (Hero bomberman : getBombermans()) {
            if (bomberman.itsMe(x, y)) {
                continue;
            }
            if (bomberman.itsMe(x, y)) {
                return Optional.of(bomberman);
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean isAnotherBomberman(Point currentPos, int newX, int newY) {
        for (Hero bomberman : getBombermans()) {
            if (bomberman.itsMe(currentPos)) {
                continue;
            }
            if (bomberman.itsMe(PointImpl.pt(newX, newY))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isMeetChopper(Point pos) {
        for (MeatChopper chopper : choppers) {
            if (chopper.itsMe(pos)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<Hero> getBombermans() {
        List<Hero> result = new LinkedList<Hero>();
        for (Player player : players) {
            result.add(player.getBomberman());
        }
        return result;
    }

    @Override
    public void remove(Player player) {
        players.remove(player);
        getRelevantPlayersRegistry(player).remove(player);
    }

    public void newGame(Player player) {
        if (!players.contains(player)) {
            players.add(player);
        }

        List<Player> relevantPlayersRegistry = getRelevantPlayersRegistry(player);
        if (!relevantPlayersRegistry.contains(player)) {
            relevantPlayersRegistry.add(player);
        }
        player.newHero(this);
    }

    private List<Player> getRelevantPlayersRegistry(Player player) {
        return player.isBot() ? botPlayers : nonBotPlayers;
    }

    public BoardReader reader() {
        return new BoardReader() {
            private int size = Bomberman.this.size();

            @Override
            public int size() {
                return size;
            }

            @Override
            public Iterable<? extends Point> elements() {
                List<Point> result = new LinkedList<Point>();
                result.addAll(Bomberman.this.getBombermans());
                for (Wall wall : Bomberman.this.getWalls()) {
                    result.add(wall);
                }
                result.addAll(Bomberman.this.getBombs());
                result.addAll(Bomberman.this.getBlasts());
                return result;
            }
        };
    }
}
