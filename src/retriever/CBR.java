package retriever;

import java.util.Collection;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import jcolibri.casebase.LinealCaseBase;
import jcolibri.cbraplications.StandardCBRApplication;
import jcolibri.cbrcore.Attribute;
import jcolibri.cbrcore.CBRCase;
import jcolibri.cbrcore.CBRCaseBase;
import jcolibri.cbrcore.CBRQuery;
import jcolibri.cbrcore.Connector;
import jcolibri.connector.PlainTextConnector;
import jcolibri.exception.ExecutionException;
import jcolibri.method.retrieve.RetrievalResult;
import jcolibri.method.retrieve.NNretrieval.NNConfig;
import jcolibri.method.retrieve.NNretrieval.NNScoringMethod;
import jcolibri.method.retrieve.NNretrieval.similarity.global.Average;
import jcolibri.method.retrieve.NNretrieval.similarity.local.Equal;
import jcolibri.method.retrieve.NNretrieval.similarity.local.MaxString;
import jcolibri.method.retrieve.selection.SelectCases;

public class CBR implements StandardCBRApplication {
	
	Connector _connector;
	CBRCaseBase _caseBase;
	
	private static JTextField _id = new JTextField(20);
	private static JTextField _titulo = new JTextField(20);
	private static JTextField _descricao = new JTextField(20);
	private static JTextField _entrada = new JTextField(20);
	private static JTextField _saida = new JTextField(20);
	private static JTextField _categoria = new JTextField(20);
	private static JTextField _topico = new JTextField(20);
	private static JTextField _nivel = new JTextField(20);
	
	@Override
	public void configure() throws ExecutionException {
		
		try {
			_connector = new PlainTextConnector();	//indica qual modelo você usará para descrever a estrutura do banco
			_connector.initFromXMLfile(jcolibri.util.FileIO.findFile("config/plainTextConnectorConfig.xml"));	//diretório do modelo no projeto
			_caseBase = new LinealCaseBase();	//lista para guardar casos que formarão a base
		} catch(Exception e) {
			System.out.println(e.getMessage());
			throw new ExecutionException(e);
		}
	}
	
	@Override
	public CBRCaseBase preCycle() throws ExecutionException {
		
		_caseBase.init(_connector);
		
		java.util.Collection<CBRCase> cases = _caseBase.getCases();	//faz a leitura dos problemas no arquivo indicado em "configure()"
		for(CBRCase c: cases) {
			System.out.println(c.toString());
		}
		
		return _caseBase;
	}

	@Override
	public void cycle(CBRQuery query) throws ExecutionException {
		
		NNConfig simConfig = new NNConfig();	//escolhe o algoritmo k-NN para cálculo de similaridade
		simConfig.setDescriptionSimFunction(new Average());	//seta "Average()" como cálculo de similariade global
		
		//aqui são criados os atributos (de acordo com o que está em "ProblemDescription.class") e setados seus pesos, além do método para calcular sua similaridade
		//Exemplo: descrição usa "MaxString()" para calcular similaridade, e tem peso 3.0
		Attribute descricao = new Attribute("descricao", ProblemDescription.class);
		simConfig.addMapping(descricao, new MaxString());
		simConfig.setWeight(descricao, 3.0);
		
		Attribute entrada = new Attribute("entrada", ProblemDescription.class);
		simConfig.addMapping(entrada, new MaxString());
		simConfig.setWeight(entrada, 1.0);
		
		Attribute saida = new Attribute("saida", ProblemDescription.class);
		simConfig.addMapping(saida, new MaxString());
		simConfig.setWeight(saida, 1.0);
		
		Attribute categoria = new Attribute("categoria", ProblemDescription.class);
		simConfig.addMapping(categoria, new Equal());
		simConfig.setWeight(categoria, 5.0);
		
		Attribute topico = new Attribute("topico", ProblemDescription.class);
		simConfig.addMapping(topico, new MaxString());
		simConfig.setWeight(topico, 5.0);
		
		Attribute nivel = new Attribute("nivel", ProblemDescription.class);
		simConfig.addMapping(nivel, new Equal());
		simConfig.setWeight(nivel, 1.0);
		
		//imprime caso de entrada
		System.out.println("Caso de entrada:");
		System.out.println(query.getDescription());
		System.out.println();
		
		//evalua os cálculos realizados, ordenando os problemas de acordo com os que foram mais similares ao caso de entrada
		Collection<RetrievalResult> eval = NNScoringMethod.evaluateSimilarity(_caseBase.getCases(), query, simConfig);
		eval = SelectCases.selectTopKRR(eval, 5);	//seta k = 5, ou seja, retorna os 5 problemas mais similares
		
		//imprime o resultado
		System.out.println("Casos recuperados:");
		for(RetrievalResult nse: eval)
			System.out.println(nse);
	}

	@Override
	public void postCycle() throws ExecutionException {
		
		this._caseBase.close();
	}
	
	static class Retrieve implements ActionListener {

		CBR cbr;
		
		public Retrieve(CBR cbr) {
			this.cbr = cbr;
		}
		
		public void actionPerformed(ActionEvent e) {
			try {
				
				//aqui é setada a descrição do problema de entrada, ou seja, seus atributos, através do que foi digitado nos textfields
				ProblemDescription queryDesc = new ProblemDescription();
				queryDesc.setId(_id.getText());	//o campo ID é opcional já que não é usado na recuperação
				queryDesc.setTitulo(_titulo.getText());
				queryDesc.setDescricao(_descricao.getText());
				queryDesc.setEntrada(_entrada.getText());
				queryDesc.setSaida(_saida.getText());
				queryDesc.setCategoria(_categoria.getText());
				queryDesc.setTopico(_topico.getText());
				queryDesc.setNivel(Integer.parseInt(_nivel.getText()));
				
				//elabora uma "query", ou seja, uma requisição para iniciar a recuperação
				CBRQuery query = new CBRQuery();
				query.setDescription(queryDesc);
				
				//inicia o ciclo de recuperação, setando os pesos de cada atributo e usando k-NN para calcular similaridade
				cbr.cycle(query);
				
				//encerra o ciclo de recuperação fechando a base de problemas
				System.out.println("Fim do ciclo.");
				cbr.postCycle();
				
			} catch (ExecutionException e1) {
				e1.printStackTrace();
			}
		}
		
	}
	
	public static void main(String[] args) {
		
		CBR cbr = new CBR();	//instancia novo problema CBR (case-based reasoning, raciocínio baseado em casos)
		
		try {
			/* ANTIGO: ESSA VERSÃO EU USEI ANTES DE FAZER A INTERFACE GRÁFICA, APENAS PRA TESTES */
			/*
			cbr.configure();
			cbr.preCycle();
			
			ProblemDescription queryDesc = new ProblemDescription();
			queryDesc.setId("1296");
			queryDesc.setTitulo("Medianas");
			queryDesc.setDescricao("Dado o comprimento das tr�s medianas de um tri�ngulo, voc� ter� que descobrir a �rea deste tri�ngulo. Se voc� n�o tem muito conhecimento sobre geometria � importante que saiba que a mediana de um tri�ngulo � formada pela conex�o de qualquer v�rtice de um tri�ngulo ao ponto central(m�dio) de sua borda oposta. Assim, um tri�ngulo tem tr�s medianas.");
			queryDesc.setEntrada("A entrada cont�m aproximadamente 1000 casos de teste. Cada caso de teste � composto por tr�s n�meros que denotam o comprimento das medianas do tri�ngulo. Todos os valores da entrada s�o menores do que 100. O final da entrada � determinado pelo final de arquivo (EOF).");
			queryDesc.setSaida("Para cada linha de entrada seu programa dever� produzir uma linha de sa�da. Esta linha deve conter a �rea do tri�ngulo para a correspondente entrada. Se n�o for poss�vel formar um tri�ngulo com as medianas fornecidas, dever� ser impresso o valor -1. As �reas devem ser arredondadas no terceiro d�gito ap�s o ponto decimal.");
			queryDesc.setCategoria("Geometria computacional");
			queryDesc.setTopico("Geometria; Tri�ngulos");
			queryDesc.setNivel(6);
			
			CBRQuery query = new CBRQuery();
			query.setDescription(queryDesc);
			
			//cbr.cycle(query);
			
			//System.out.println("Fim do ciclo.");
			//cbr.postCycle();
			
			*/
			/* ANTIGO */
			
			cbr.configure();	//inicia configuração do recuperador, para setar a estrutura e o diretório da base de problemas que irá formar o banco
			cbr.preCycle();	//constrói a base de problemas, fazendo a leitura do arquivo indicado no FilePath em "plainTextConnectorConfig.xml"
			
			/* Interface: cria janela, painel, labels, textfields, etc */
			JFrame mainWindow = new JFrame("CBR");
			mainWindow.setVisible(true);
			mainWindow.setSize(600, 600);
			mainWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			
			GridBagConstraints constraints = new GridBagConstraints();
			constraints.anchor = GridBagConstraints.WEST;
			constraints.insets = new Insets(10, 10, 10, 10);
			
			JPanel panel = new JPanel(new GridBagLayout());
			mainWindow.getContentPane().add(panel, BorderLayout.NORTH);
			
			constraints.gridx = 0;
	        constraints.gridy = 0;     
	        panel.add(new JLabel("ID: "), constraints);
	 
	        constraints.gridx = 1;
	        panel.add(_id, constraints);
	         
	        constraints.gridx = 0;
	        constraints.gridy = 1;     
	        panel.add(new JLabel("Título: "), constraints);
	         
	        constraints.gridx = 1;
	        panel.add(_titulo, constraints);
	        
	        constraints.gridx = 0;
	        constraints.gridy = 2;     
	        panel.add(new JLabel("Descrição: "), constraints);
	         
	        constraints.gridx = 1;
	        panel.add(_descricao, constraints);
	        
	        constraints.gridx = 0;
	        constraints.gridy = 3;     
	        panel.add(new JLabel("Entrada: "), constraints);
	         
	        constraints.gridx = 1;
	        panel.add(_entrada, constraints);
	        
	        constraints.gridx = 0;
	        constraints.gridy = 4;     
	        panel.add(new JLabel("Saída: "), constraints);
	         
	        constraints.gridx = 1;
	        panel.add(_saida, constraints);
	        
	        constraints.gridx = 0;
	        constraints.gridy = 5;     
	        panel.add(new JLabel("Categoria: "), constraints);
	         
	        constraints.gridx = 1;
	        panel.add(_categoria, constraints);
	        
	        constraints.gridx = 0;
	        constraints.gridy = 6;     
	        panel.add(new JLabel("Tópico: "), constraints);
	         
	        constraints.gridx = 1;
	        panel.add(_topico, constraints);
	        
	        constraints.gridx = 0;
	        constraints.gridy = 7;     
	        panel.add(new JLabel("Nível: "), constraints);
	         
	        constraints.gridx = 1;
	        panel.add(_nivel, constraints);
	         
	        panel.setBorder(BorderFactory.createTitledBorder(
	                BorderFactory.createEtchedBorder(), "Retriever panel"));
			
			JButton button = new JButton("Retrieve");
			Retrieve retrieve = new Retrieve(cbr);	//essa classe realiza a descrição do problema de entrada, ou seja, seta seus atributos, recebendo o problema CBR instanciado
			button.addActionListener(retrieve);	//esse método é invocado quando o botão é clicado, para iniciar a recuperação partindo do caso descrito
			
			constraints.gridx = 0;
	        constraints.gridy = 8;
	        constraints.gridwidth = 2;
	        constraints.anchor = GridBagConstraints.CENTER;
			panel.add(button, constraints);
	        
		} catch(Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}

}
