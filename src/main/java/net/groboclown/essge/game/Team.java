/**
 * Eternity Keeper, a Pillars of Eternity save game editor.
 * Copyright (C) 2015 the authors.
 * <p>
 * Eternity Keeper is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * <p>
 * Eternity Keeper is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */


package net.groboclown.essge.game;

import net.groboclown.essge.game.Faction.Relationship;
import net.groboclown.essge.sharp.serializer.CSharpCollection;

import static net.groboclown.essge.game.Reputation.ChangeStrength;
import static net.groboclown.essge.game.UnityEngine.HideFlags;

public class Team {
    public String name;
    public HideFlags hideFlags;
    public Relationship DefaultRelationship;
    public FactionName GameFaction;
    public ChangeStrength InjuredReputationChange;
    public ChangeStrength MurderedReputationChange;
    public CSharpCollection FriendlyTeams;
    public CSharpCollection HostileTeams;
    public CSharpCollection NeutralTeams;
    public boolean RestoredTeam;
    public String ScriptTag;
    public String m_scriptTag;
    public String Tag;
}
