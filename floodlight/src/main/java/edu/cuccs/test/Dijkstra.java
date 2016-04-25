package edu.cuccs.test;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;





class Link{
	int left;
	int right;
	int bandwidth;
	
	public Link(int l, int r, int b){
		left = l;
		right = r;
		bandwidth = b;
	}
	
	public void print(){
		System.out.println(left+"->"+right);
	}
}

public class Dijkstra {

	final static int MAX = 99999;
	static List<Link> map = new ArrayList<Link>();	
	static List<Link> target = new ArrayList<Link>();
	static int[][] bwG;	//������
	static int[][] conG;	//��ͨ����
	static int n;			//�����
    
	static boolean dijkstra( Link t, int[] dist, int[] prev){
		
		boolean[] flag = new boolean[n+1];
		
		for(int i=1; i<=n; i++){
			/*if(bwG[t.left][i]>=t.bandwidth)
				dist[i] = conG[t.left][i];
			else
				dist[i] = MAX;*/
			dist[i] = bwG[t.left][i]>=t.bandwidth?conG[t.left][i]:MAX;
			flag[i] = false;
			prev[i] = dist[i]==MAX?0:t.left;
		}
		dist[t.left] = 0;
		flag[t.left] = true;
		
		int sign=0;
		for(int i=1; i<=n; i++){
			if(dist[i]==MAX)
				sign++;
		}
		if(sign==n)
			return false;
		
		
		
		for(int i=2; i<=n; i++){
			int tmp = MAX;
			int u=t.left;
			for(int j=1; j<=n; j++){
				if( !flag[j] && dist[j]<tmp ){
					u = j;
					tmp = dist[j];
				}
			}
			flag[u] = true;
			
			for(int j=1; j<=n; j++){
				if( !flag[j] && (conG[u][j]+tmp)<dist[j] && bwG[u][j]>=t.bandwidth){
					dist[j] = conG[u][j]+tmp;
					prev[j] = u;
				}
			}
		}
		if(dist[t.right] == MAX)
			return false;
		return true;
	}
	
	static String searchPath(int[] prev, Link t){
		
		String result = "";
		int[] que = new int[n+1];
		int c = 1;
		que[c] = t.right;
		c++;
		int tmp = prev[t.right];
		while( tmp != t.left ){
			que[c] = tmp;
			c++;
			tmp = prev[tmp];
		}
		que[c] = t.left;
		for(int i=c; i>=1; i--){
			if(i!=1){
				//System.out.print(que[i]+"\n");
				result += ( que[i] + ",");
				bwG[que[i]][que[i-1]] = bwG[que[i]][que[i-1]] - t.bandwidth;
				bwG[que[i-1]][que[i]] = bwG[que[i]][que[i-1]];
			}
			else
				result += que[i];				
		}
		return result;
	}
	

	public int[] getroute(int left,int right)
	{
		convert();
		String write = "";
		Link link = new Link(left,right,100);
    	target.add(link);
		int[] dist = new int[n+1];
		int[] prev = new int[n+1];
		//System.out.println("target---"+target.size());
		
		boolean found = dijkstra(target.get(0),dist,prev);
		if(!found)
			return null;
		String tmp = searchPath(prev,target.get(0));
	    String[] t = tmp.split(",");
		int[] in = new int[t.length];
		for(int i=0;i<t.length;i++)
		{
			in[i] = Integer.parseInt(t[i]);
		}
		target.remove(0);
		return in;
	}
	public void addlink(int left,int right,int daijia)
	{
		 Link link = new Link(left,right,daijia);
    	 Link reverseLink = new Link(right,left,daijia);
    	 map.add(link);
    	 map.add(reverseLink);
    	 n = (n>link.left)?n:link.left;
    	 n = (n>link.right)?n:link.right;
	}
	/*public void ma(int[] left,int[]right,int daijia) { 
		for(int i = 0;i<left.length;i++)
			     {
			    	 Link link = new Link(left[i],right[i],daijia);
			    	 Link reverseLink = new Link(right[i],left[i],daijia);
			    	 map.add(link);
			    	 map.add(reverseLink);
			    	 n = (n>link.left)?n:link.left;
			    	 n = (n>link.right)?n:link.right;
			     }	
	}
    */
	private static void convert() {
		bwG = new int[n+1][n+1];
		conG = new int[n+1][n+1];
		for(int i=1; i<=n; i++){
			for(int j=1; j<=n; j++){
				bwG[i][j] = MAX;
				conG[i][j] = MAX;
			}
		}
		for(int i=0; i<map.size(); i++){
			Link l = map.get(i);
			bwG[l.left][l.right] = l.bandwidth;
			conG[l.left][l.right] = 1;
		}
		
	}
    private void read2(int[] left,int[] right,int daijia)
    {
     for(int i = 0;i<left.length;i++)
     {
    	 Link link = new Link(left[i],right[i],daijia);
    	 Link reverseLink = new Link(right[i],left[i],daijia);
    	 map.add(link);
    	 map.add(reverseLink);
    	 n = (n>link.left)?n:link.left;
    	 n = (n>link.right)?n:link.right;
     }
     
   	 
   	 
    }
	

}
