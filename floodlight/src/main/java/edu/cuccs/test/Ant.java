package edu.cuccs.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class Ant {
	
	int n;				//�ܽ����
	List<Integer> path;	//·��
	boolean[] map;		//�����Ƿ񾭹��㣬������true��δ������false
	int curNode;		//��ǰ���
	
	Link target;
	boolean succeed;	//��ʶ�����Ƿ�ɹ��ҵ�·��
	
	final static double Alpha = 1.0;	//��Ϣ����ʽ���ӣ���Ϣ�ص���Ҫ�̶�
	final static double Beta = 1.0;		//��������ʽ���ӣ��ڵ��������Ҫ�̶�
	final static double ROU = 0.1;		//�ֲ�������Ϣ�ػӷ����ӣ�0��1
	final static double Q = 1;			//��Ϣ��
	
	public Ant( int n, Link target){
		this.n = n;
		this.target = target;
		this.curNode = target.left;
		path = new ArrayList<Integer>();
		map = new boolean[n];
		
	}
	
	//��ʼ��
	void init(){
		for(int i=0; i<n; i++)
			map[i] = false;
		map[curNode] = true;
		path.add(curNode);
	}
	
	//����ѡ����һ�����
	int chooseNext(){
		int result = -1;
		double pheTotal = 0.0;				//���㵱ǰ����û�߹�Ľڵ�֮�����Ϣ���ܺ�
		double[] prob = new double[n+1];	//��������ڵ㱻ѡ�еĸ���
		
		for(int i=0; i<n; i++){
			if(map[i] || AA.conG[curNode][i]!=1 ||AA.bwG[curNode][i]<target.bandwidth){	
				//���ǰ���ȥ����߲���ͨ���ߴ�?��
				prob[i] = 0.0; 
			}
			else{
				//��ݶ�Ӧ����Թ�ʽ���м򻯸���
				//prob[i] = Math.pow(Run.pheG[curNode][i], Alpha)*Math.pow(1.0/Run.conG[curNode][i], Beta);
				prob[i] = Math.pow(AA.pheG[curNode][i], Alpha);
				pheTotal += prob[i];
			}
		}
		//��������ѡ��
		double tmp = 0.0;
		if( pheTotal > 0.0){
			tmp = getRanDou(0.0,pheTotal);
			for(int i=0; i<n; i++)
			if(map[i]==false){
				tmp = tmp - prob[i];
				if(tmp<0.0) {	result = i;	break;	}
			}
		}
		return result;
	}
	
	//�����ƶ�
	boolean move( ){
		int next = chooseNext();
		if( next == -1)		return false;	//����ǰ·���Ѿ��޷���ͨ
		this.curNode = next;
		this.path.add(curNode);
		map[curNode] = true;
		return true;
	}
	
	//����Ѱ��·��
	void search(){
		init();
		boolean flag = true;
		while(flag && curNode!=target.right){
			flag = move();
		}
		if(flag==false)		succeed = false;
		else	succeed = true;
	}
	
	//�ֲ�������Ϣ��
	void update(){
		for(int i=0; i<n; i++){
			for(int j=0; j<n; j++)
				AA.pheG[i][j] *= (1-ROU);
		}
		int pathLen = path.size();
		if(pathLen>0){
			for(int i=0; i<path.size()-1; i++){
				AA.pheG[path.get(i)][path.get(i+1)] += ROU*(Q/path.size());
			}
		}
	}
	
	//����·������
	public int getPathLen(){
		return path.size();
	}
	
	//����ָ����Χ�ڵ�������
	double getRanDou(double low,double upper){
		Random rand = new Random();
		double randNum = rand.nextDouble()*(upper-low) + low;
		return randNum;
	}
}