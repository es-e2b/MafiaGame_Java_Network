package MafiaGame;

import MafiaGame.Mafia_Integrated.ClientHandler;

public class Role_Observer extends Role{
	String role = "관전자";

	public Role_Observer(ClientHandler player) {
		super(player);
		this.player.sendToClient(new ChatMsg(ChatMsg.MODE_CONTROL,ChatMsg.CODE_ROLE,role));
	}

	@Override
	public String ability(String nickname) {
		return "관전자는 능력이 없습니다.";
	}

	@Override
	public void notifyCreation() {
		String notify="\n\n***************************************************\n\n"+
				"당신은 관전자입니다.\n\n"+
				"***************************************************\n";
		player.sendSystemMessageToClient(notify);
	}

	public String toString() {
		return role;
	}
}
