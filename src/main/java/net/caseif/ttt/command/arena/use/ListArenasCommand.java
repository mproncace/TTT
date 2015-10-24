/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2013-2015, Maxim Roncacé <mproncace@lapis.blue>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package net.caseif.ttt.command.arena.use;

import net.caseif.ttt.TTTCore;
import net.caseif.ttt.command.SubcommandHandler;
import net.caseif.ttt.util.Constants.Color;

import net.caseif.flint.arena.Arena;
import org.bukkit.command.CommandSender;

public class ListArenasCommand extends SubcommandHandler {

    public ListArenasCommand(CommandSender sender, String[] args) {
        super(sender, args, "ttt.use");
    }

    @Override
    public void handle() {
        if (assertPermission()) {
            TTTCore.locale.getLocalizable("info.personal.arena.list").withPrefix(Color.INFO).sendTo(sender);
            for (Arena arena : TTTCore.mg.getArenas()) {
                sender.sendMessage("    " + Color.USAGE + arena.getId() + ": "
                        + Color.DESCRIPTION + TTTCore.locale.getLocalizable("fragment.stage."
                        + (arena.getRound().isPresent()
                        ? arena.getRound().get().getLifecycleStage().getId()
                        : "waiting"))
                        .localizeFor(sender).toUpperCase());
            }
        }
    }

}