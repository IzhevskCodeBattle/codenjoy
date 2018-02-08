package com.codenjoy.dojo.kata.model.levels.algorithms.finale;

/*-
 * #%L
 * Codenjoy - it's a dojo-like platform from developers to developers.
 * %%
 * Copyright (C) 2016 - 2018 Codenjoy
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



import com.codenjoy.dojo.kata.model.levels.algorithms.LongDivisionAlgorithm;

import java.util.Arrays;
import java.util.List;

public class FinaleLongDivisionAlgorithm extends LongDivisionAlgorithm {

    @Override
    public List<String> getQuestions() {
        return Arrays.asList(
                "111, 11",
                "11111, 11",
                "-11, -222",
                "111, -22",
                "1, 3000",
                "87, 78",
                "45, 56",
                "212, 133",
                "11111, 115",
                "123, 345",
                "66666666, 77727777",
                "666666660, 77727777",
                "666666660, 7772777",
                "100, 97",
                "999, 0"
        );
    }
}
