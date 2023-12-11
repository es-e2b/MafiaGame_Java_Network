package MafiaGame;

import java.util.*;
import java.util.Map.Entry;

public class GameState_Voting extends GameState {
//	Mafia game;
	String state="투표";
//	int time = 60; // 기본 60초
	int time = Mafia.votingTime;
//	private Timer timer=new Timer();
	String Nominee=null;
	boolean isDebate=false;
	boolean isVotingTime=true;
	boolean isApprovalTime=false;

	HashMap<String, String> votingList;
	HashMap<String, Integer> vote;

	GameState_Voting() {
		super();
		if(isEnded)
			return;
		startVote();
	}
	void notifyCreation() {
		Mafia_Integrated.broadcasting(new ChatMsg(ChatMsg.CODE_VOTING));
		Mafia_Integrated.broadcastingSystem("\n투표가 시작되었습니다.\n"
				+ "투표하고 싶은 대상의 닉네임을 '/닉네임'으로 입력하세요.\n"
				+ "여러 번 투표할 수 있으며 마지막으로 지목한 대상만 투표 목록에 올라갑니다.\n");
	}
	String resultAbility(String user, String nominee) {
		if(user.equals(Nominee))
			return "최다 득표자는 투표할 수 없습니다.";
		else if(isApprovalTime&&(nominee.equals("Y") || nominee.equals("N"))) {
			votingList.put(user, nominee);
			return "["+nominee+"] 투표";
		}
		else if(isVotingTime&&Mafia.roles.containsKey(nominee)
				&&!Mafia.roles.get(nominee).toString().equals("사망")
				&&!Mafia.roles.get(nominee).toString().equals("관전자")) {
			votingList.put(user, nominee);
			return "["+nominee+"] 투표";
		}
		else {
			return "잘못된 입력입니다.";
		}
	}
	void startVote() {
		votingList = new HashMap<>();
		vote=new HashMap<>();
		
		Mafia_Integrated.broadcastingSystem(time+"초 남았습니다.");
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
            	if(time==30)
            		Mafia_Integrated.broadcastingSystem(time+"초 남았습니다.");
            	else if(time==10)
            		Mafia_Integrated.broadcastingSystem(time+"초 남았습니다.");
            	else if(time==0) {
                    if(resultVote()) {
                    	debateTime();
                    	isVotingTime=false;
                    }
                    else
                    	nextState();
            		this.cancel();
            	}
            	time--;
            }
        }, 0, 1000); // 0초부터 시작하여 1초 간격으로 실행
	}
	public boolean resultVote() {
		int max=0;
		for(String nominee : votingList.values()) {
		    int currentVotes = vote.getOrDefault(nominee, 0) + 1;
		    vote.put(nominee, currentVotes);
		    max = Math.max(max, currentVotes);
		}
		
		PriorityQueue<String> pq = new PriorityQueue<>(new Comparator<String>() {

			@Override
			public int compare(String player1, String player2) {
				
				return Integer.compare(vote.get(player1), vote.get(player2));
			}
			
		});
		
		for(String nickname : vote.keySet())
			pq.offer(nickname);
		while(!pq.isEmpty()) {
			String now = pq.poll();
			if(vote.get(now)==max) {
				if(Nominee==null)
					Nominee=now;
				else
					Nominee="None";
			}
			Mafia_Integrated.broadcastingSystem("["+now+"] - "+vote.get(now)+"\n");
		}
		if(Nominee==null||Nominee.equals("None")) {
			Mafia_Integrated.broadcastingSystem("부결되었습니다.");
			return false;
		}
		else {
			Mafia_Integrated.broadcastingSystem("["+Nominee+"]"+" 지목되었습니다.");
			return true;
		}
	}
	private void debateTime() {
		isDebate=true;
		Mafia_Integrated.broadcasting(new ChatMsg(ChatMsg.CODE_VOTING));
		Mafia_Integrated.players.get(Nominee).sendToClient(new ChatMsg(ChatMsg.CODE_DAY));
		Mafia_Integrated.broadcastingSystem("최후의 변론 시간입니다.\n 20초 남았습니다.");
	    timer.schedule(new TimerTask() {
	        @Override
	        public void run() {
	        	isDebate=false;
	        	Mafia_Integrated.players.get(Nominee).sendToClient(new ChatMsg(ChatMsg.CODE_VOTING));
	        	startApprovalVote();
	        }
	    }, 20000);
	}
	void startApprovalVote() {
		isApprovalTime=true;
		votingList=new HashMap<>();
		vote = new HashMap<>();
		vote.put("Y", 0);
		vote.put("N", 0);
		String notify = "찬반 투표가 시작되었습니다.\n"
				+ "/Y 또는 /N 을 통해 투표할 수 있습니다.\n"
				+ "사형을 원하시면 /Y , 원치 않으시면 /N 를 입력해주세요.";
		Mafia_Integrated.broadcastingSystem(notify);
		Mafia_Integrated.broadcastingSystem(10+"초 남았습니다.");
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
        		for(String approval : votingList.values()) {
        		    vote.put(approval, vote.getOrDefault(approval, 0) + 1);
        		}
            	if(resultApprovalVote()) {
            		Mafia_Integrated.broadcastingSystem("["+Nominee+"]"+" 사형되었습니다.");
            		Mafia.deadList.add(Nominee);
        			Mafia.roles.put(Nominee, new Role_Dead(Mafia_Integrated.players.get(Nominee)));
        			Mafia.broadcastingToDead(new ChatMsg(ChatMsg.MODE_DEADCHAT, ChatMsg.CODE_UPDATE, Mafia.deadList()));
            	}
            	else
            		Mafia_Integrated.broadcastingSystem("부결되었습니다.");
            	nextState();
            }
        }, 10000);
	}
	private boolean resultApprovalVote() {
		int approval = vote.get("Y");
		int disapproval= vote.get("N");
		Mafia_Integrated.broadcastingSystem("찬성 : "+approval +
				"\n반대 : " + disapproval);
		return approval>disapproval;
	}
	@Override
	void readMessage(ChatMsg msg) {
		if(isDebate&&msg.nickname.equals(Nominee)) { // 변론 시간엔 변론자만 채팅 가능.
			Mafia_Integrated.broadcasting(msg);
			return;
		}
		Mafia_Integrated.players.get(msg.nickname).sendToClient(new ChatMsg(ChatMsg.MODE_SYSTEM, "투표 시간엔 대화할 수 없습니다."));
	}
	@Override
	void nextState() {
		Mafia.state=new GameState_Night();
	}

}
