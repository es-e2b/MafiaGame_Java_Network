package MafiaGame;

import MafiaGame.Mafia_Integrated.ClientHandler;

public abstract class Role {
	String role;
	ClientHandler player;
	public Role(ClientHandler player) {
		this.player=player;
		notifyCreation();
	}
	public String toString() {
		return role;
	}
	public abstract String ability(String nickname);
	public abstract void notifyCreation();
}
