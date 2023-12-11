package MafiaGame;

import java.util.Timer;

abstract class GameState {
	protected Timer timer;
	String state; //"낮" & "밤" & "투표"
	boolean isEnded=false;
	GameState() {
		isEnded=checkVictory();
		if(isEnded)
			return;
		this.timer=new Timer();
		Mafia_Integrated.controlUpdate();
		notifyCreation();
	}
	public String toString() {
		return state;
	}
	abstract void nextState();
	abstract String resultAbility(String nickname, String message);
	abstract void readMessage(ChatMsg msg);
	abstract void notifyCreation();
	boolean checkVictory() {
		int mafia=0;
		int civilian=0;
		for(Role role : Mafia.roles.values()) {
			if(role.toString().equals("마피아"))
				mafia++;
			else if(role.toString().equals("관전자")||role.toString().equals("사망"))
				continue;
			else
				civilian++;
		}
		if(mafia>=civilian) {
			Mafia_Integrated.broadcastingSystem("\n\n*******************************************\n\n"
					+ "마피아가 승리하였습니다!"
					+ "\n\n*******************************************\n\n");
//			timer.cancel();
			Mafia_Integrated.endGame();
			return true;
		}
		else if(mafia==0) {
			Mafia_Integrated.broadcastingSystem("\n\n*******************************************\n\n"
					+ "시민이 승리하였습니다!"
					+ "\n\n*******************************************\n\n");
//			timer.cancel();
			Mafia_Integrated.endGame();
			return true;
		}
		else
			return false;
	}
}
