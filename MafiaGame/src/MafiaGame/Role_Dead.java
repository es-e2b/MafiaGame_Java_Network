package MafiaGame;

import MafiaGame.Mafia_Integrated.ClientHandler;

public class Role_Dead extends Role{
	String role = "사망";

	public Role_Dead(ClientHandler player) {
		super(player);
		player.sendToClient(new ChatMsg(ChatMsg.CODE_DEATH));
		this.player.sendToClient(new ChatMsg(ChatMsg.MODE_CONTROL,ChatMsg.CODE_ROLE,role));
	}

	@Override
	public String ability(String nickname) {
		return "사망자는 능력이 없습니다.";
	}

	@Override
	public void notifyCreation() {
		String notify="\n\n***************************************************\n\n"+
				"당신은 죽었습니다.\n\n"+
				"***************************************************\n";
		player.sendSystemMessageToClient(notify);
	}

	public String toString() {
		return role;
	}
}
