package MafiaGame;

import MafiaGame.Mafia_Integrated.ClientHandler;

public class Role_Police extends Role {
	String role = "경찰";

	public Role_Police(ClientHandler player) {
		super(player);
		this.player.sendToClient(new ChatMsg(ChatMsg.MODE_CONTROL,ChatMsg.CODE_ROLE,role));
	}

	@Override
	public String ability(String nickname) {
		Role role = Mafia.roles.get(nickname);
		if (role instanceof Role_Mafia) {
			return"[" + nickname + "]" + "님은 마피아 입니다.";
		} else {
			return"[" + nickname + "]" + "님은 시민 입니다.";
		}

	}

	@Override
	public void notifyCreation() {
		String notify=
				"\n\n***************************************************\n\n"+
				"당신은 경찰입니다.\n\n"
				+ "마피아를 조사해서 진실을 밝히세요.\n\n"+
				"***************************************************\n";
		player.sendSystemMessageToClient(notify);
	}
	public String toString() {
		return role;
	}

}
