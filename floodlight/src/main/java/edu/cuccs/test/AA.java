package edu.cuccs.test;

import java.util.ArrayList;
import java.util.List;

public class AA {
	
	final   int MAX = 99999;
	
	int antNum = 20;
	final   int count = 500;
	
	final   double Cinit = 1.0;	//��ʼ��Ϣ��Ũ��
	final   double MIU = 0.1;	//ȫ�ָ�����Ϣ�ػӷ����ӣ�0��1

	
	List<Link> map = new ArrayList<Link>();	
	static int[][] bwG;		//������
	static int[][] conG;	//��ͨ����
	static double[][] pheG;	//��Ϣ�ؾ���
	int n;					//�����
	
	
	
	public void addlink(int left,int right,int bw)
	{
		 Link link = new Link(left,right,bw);
    	 Link reverseLink = new Link(right,left,bw);
    	 map.add(link);
    	 map.add(reverseLink);
    	 n = ((n>link.left)?n:link.left) + 1;	//��������������ż�һ����0��
    	 n = ((n>link.right)?n:link.right) + 1;
    	//��ݽ����n����·��ȷ��������ĿantNum�͵�����
    	 antNum = (n+map.size()/2)*4;
	}
	
	public int[] getRoute(int left,int right,int bw)
	{
		convert();
		Link link = new Link(left,right,bw);
		ArrayList<Ant> result = new ArrayList<Ant>();
		for(int j=0; j<count; j++){
			Ant bestAnt = search(link);
			if(bestAnt!=null){
				update(bestAnt);
				result.add(bestAnt);
			}
		}
		
		if(result.isEmpty())	return null;
		else{
			int length = MAX;
			int index = 0;
			for(int k=0; k<result.size(); k++){
				if(k==0){
					index=0;
					length=result.get(0).getPathLen();
				}
				int tmp = result.get(k).getPathLen();
				if( tmp < length){
					index = k;
					length = tmp;
				}
			}
			Ant ant = result.get(index);
			int[] route = new int[ant.getPathLen()];
			int i=0;
			for(i=0; i<ant.getPathLen()-1; i++){
				route[i] = ant.path.get(i);
				//���´�����
				bwG[ant.path.get(i)][ant.path.get(i+1)] -= bw;
				bwG[ant.path.get(i+1)][ant.path.get(i)] = bwG[ant.path.get(i)][ant.path.get(i+1)];
			}
			route[i] = ant.path.get(i);
			//������Ϣ�ؾ���
			pheG = new double[n][n];
			for(int k=0; k<n; k++){
				for(int j=0; j<n; j++){
					pheG[k][j] = Cinit;
				}
			}
			return route;
		}
	}
	
	//���һ������������ϸ�����Ϣ��
	 void update(Ant bestAnt){
		 for(int i=0; i<n; i++){
			for(int j=0; j<n; j++)
				pheG[i][j] *= (1-MIU);
		 }
		 int pathLen = bestAnt.path.size();
		 if(pathLen>0){
			 for(int i=0; i<pathLen-1; i++){
				 pheG[bestAnt.path.get(i)][bestAnt.path.get(i+1)] += MIU*(1/pathLen);
			 }
		 }
	 }
	
	//���һ��������������
	Ant search(Link t) {
		boolean flag = false;
		Ant bestAnt = null;
		List<Ant> ants = new ArrayList<Ant>();
		for(int j=0; j<antNum; j++){
			ants.add(new Ant(n,t));
		}
		for(int j=0; j<ants.size(); j++){
			Ant ant = ants.get(j);
			ant.search();
			if(ant.succeed){
				if(flag==false){ bestAnt = ant;	flag = true;}
				else if(bestAnt.getPathLen()>ant.getPathLen())
					bestAnt = ant;
			}
		}	
		return bestAnt;
	}
	
	void convert() {
		bwG = new int[n][n];
		conG = new int[n][n];
		pheG = new double[n][n];
		for(int i=0; i<n; i++){
			for(int j=0; j<n; j++){
				bwG[i][j] = MAX;
				conG[i][j] = MAX;
				pheG[i][j] = Cinit;
			}
		}
		for(int i=0; i<map.size(); i++){
			Link l = map.get(i);
			bwG[l.left][l.right] = l.bandwidth;
			conG[l.left][l.right] = 1;
		}	
	}
}
