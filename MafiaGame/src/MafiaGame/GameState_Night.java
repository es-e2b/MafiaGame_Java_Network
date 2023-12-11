package MafiaGame;

import java.util.*;

public class GameState_Night extends GameState {
	String state = "밤";
//	private Timer timer=new Timer();
	int time = Mafia.nightTime;
	
	HashMap<String, String> attackList = new HashMap<>(); // 여러 번 지목할 수 있으며, 지목하면 업데이트 됨.
	HashMap<String, String> healList = new HashMap<>(); // 여러 번 지목할 수 있으며, 지목하면 업데이트 됨.
	HashSet<String> investigateList = new HashSet<>(); // 나중에 역할이 많아졌을 시 한 사람당 한 번만 조사하도록.
	String Nominee; // 공격 대상
	
	GameState_Night() {
		super();
		if(isEnded)
			return;
		Mafia_Integrated.broadcastingSystem(time+"초 남았습니다.");
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
            	if(time==30)
            		Mafia_Integrated.broadcastingSystem(time+"초 남았습니다.");
            	else if(time==10)
            		Mafia_Integrated.broadcastingSystem(time+"초 남았습니다.");
            	else if(time==0) {
            		attackResult();
            		nextState();
            	}
            	time--;
            }
        }, 0, 1000); // 0초부터 시작하여 1초 간격으로 실행
		
	}

	String resultAbility(String nickname, String message) {
		if(!Mafia.roles.containsKey(message) // 만약 대상이 없는 플레이어거나
				|| Mafia.roles.get(message) instanceof Role_Dead // 죽은 플레이어거나
				|| Mafia.roles.get(message) instanceof Role_Observer)// 관전자라면
			return "잘못된 대상입니다."; // 잘못된 대상 출력
		
		if(Mafia.roles.get(nickname) instanceof Role_Doctor) // 능력을 발휘한 사람이 의사라면
			healList.put(nickname, message); // 힐 리스트에 대상 추가. 여러번 사용하면 마지막으로 지목한 대상만 치료.
		else if(Mafia.roles.get(nickname) instanceof Role_Mafia) { // 능력을 발휘한 사람이 마피아라면
			attackList.put(nickname, message); // 공격 리스트에 대상 추가. 여러번 사용하면 마지막으로 지목한 대상만 공격.
			Mafia.broadcastingToMafia(new ChatMsg(ChatMsg.MODE_SYSTEM,
					"["+nickname+"] 님이 ["+message+"] 님을 지목했습니다."));
		}
		else if(Mafia.roles.get(nickname) instanceof Role_Police){// 능력을 발휘한 사람이 경찰이라면
			if(investigateList.contains(nickname)) // 이미 수사했는지 조사하고,
				return "더 이상 수사할 수 없습니다."; // 수사했다면 메세지 리턴.
			investigateList.add(nickname);	// 처음이라면 수사 목록에 추가하여, 두 번 수사할 수 없도록 함.
		}
		String returnMessage = Mafia.roles.get(nickname).ability(message);
		return returnMessage;
	}

	void readMessage(ChatMsg msg) {
		Mafia_Integrated.players.get(msg.nickname).sendSystemMessageToClient("주무실 시간입니다.");
	}
	
	void attackResult() {
		int max=0;
		HashMap<String, Integer> vote=new HashMap<>();
		for(String nominee : attackList.values()) {
		    int currentAttack = vote.getOrDefault(nominee, 0) + 1;
		    vote.put(nominee, currentAttack);
		    max = Math.max(max, currentAttack);
		}
		PriorityQueue<String> pq = new PriorityQueue<>(new Comparator<String>() {

			@Override
			public int compare(String player1, String player2) {
				
				return Integer.compare(vote.get(player1), vote.get(player2));
			}
			
		});
		
		for(String nickname : vote.keySet()) // 공격 대상 리스트를 보여주기 위해 큐에 추가.
			pq.offer(nickname);
		while(!pq.isEmpty()) { // 큐를 돌며 공격 최다 지목자가 있으면 공격 대상으로 선언. 겹치면 None.
			String now = pq.poll();
			if(vote.get(now)==max) {
				if(Nominee==null)
					Nominee=now;
				else
					Nominee="None";
			}
			Mafia.broadcastingToMafia(new ChatMsg(ChatMsg.MODE_SYSTEM,"["+now+"] - "+vote.get(now)+"\n"));
		}
		if(Nominee==null||Nominee.equals("None")) {
			Mafia_Integrated.broadcastingSystem("아무도 공격받지 않았습니다.");
		}
		else {
			if(healList.containsValue(Nominee))
				Mafia_Integrated.broadcastingSystem("["+Nominee+"] 가 공격을 당했지만, 의사에 의해 살아났습니다.");
			else {
				Mafia_Integrated.broadcastingSystem("["+Nominee+"] 가 마피아의 공격을 받아 죽었습니다.");
				Mafia.deadList.add(Nominee);
				Mafia.roles.put(Nominee, new Role_Dead(Mafia_Integrated.players.get(Nominee)));
				Mafia.broadcastingToDead(new ChatMsg(ChatMsg.MODE_DEADCHAT, ChatMsg.CODE_UPDATE, Mafia.deadList()));
			}
		}
	}

	@Override
	void notifyCreation() {
		Mafia.broadcastingToAlive(new ChatMsg(ChatMsg.CODE_NIGHT));
		Mafia_Integrated.broadcastingSystem("\n밤이 되었습니다.\n"
				+ "능력을 사용하고 싶은 대상의 닉네임을 '/닉네임'으로 입력하세요.\n"
				+ "마피아와 의사의 경우 여러 번 투표할 수 있으며\n"
				+ "마지막으로 지목한 대상만 투표 목록에 올라갑니다.\n");
	}

	@Override
	void nextState() {
		Mafia.state = new GameState_Day();
	}
}
