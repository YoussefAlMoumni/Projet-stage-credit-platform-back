from langchain_ollama import OllamaLLM
from langchain_core.runnables import RunnableConfig
from langgraph.graph import StateGraph, START, END
from src.state import CreditState

# --- Helper to sanitize keep-alive strings ---
def format_keep_alive(val) -> str:
    if isinstance(val, int) and val > 0:
        return f"{val}s"
    return "0s"

# --- Graph Worker Nodes ---

def node_solvabilite(state: CreditState, config: RunnableConfig):
    params = config.get("configurable", {"worker_ctx": 2048, "worker_keep_alive": 0})
    
    llm = OllamaLLM(
        model="deepseek-r1:8b", 
        temperature=0.1,
        num_ctx=params["worker_ctx"],
        num_predict=256,
        keep_alive=format_keep_alive(params["worker_keep_alive"])
    )
    
    prompt = (
        f"Analyste Solvabilité:\n"
        f"Évaluer la capacité brute de remboursement pour le dossier {state['dossier_id']} "
        f"concernant un client de type '{state['type_client']}'. Extraire et analyser le ratio d'endettement.\n"
        f"Générer une recommandation structurée."
    )
    response = llm.invoke(prompt)
    return {"solvabilite_output": response}

def node_historique(state: CreditState, config: RunnableConfig):
    params = config.get("configurable", {"worker_ctx": 2048, "worker_keep_alive": 0})
    
    llm = OllamaLLM(
        model="deepseek-r1:8b", 
        temperature=0.1,
        num_ctx=params["worker_ctx"],
        num_predict=256,
        keep_alive=format_keep_alive(params["worker_keep_alive"])
    )
    
    prompt = (
        f"Analyste Historique:\n"
        f"Vérifier les incidents de paiement historiques et l'état des engagements en cours "
        f"pour le dossier {state['dossier_id']}.\n"
        f"Fournir un score de risque sur les antécédents."
    )
    response = llm.invoke(prompt)
    return {"historique_output": response}

def node_garanties(state: CreditState, config: RunnableConfig):
    params = config.get("configurable", {"worker_ctx": 2048, "worker_keep_alive": 0})
    
    llm = OllamaLLM(
        model="deepseek-r1:8b", 
        temperature=0.1,
        num_ctx=params["worker_ctx"],
        num_predict=256,
        keep_alive=format_keep_alive(params["worker_keep_alive"])
    )
    
    prompt = (
        f"Évaluateur de Garanties:\n"
        f"Évaluer la liquidité, la valeur estimée et le ratio de couverture du collatéral proposé "
        f"par rapport au montant demandé de {state['montant_demande']}.\n"
        f"Rédiger un avis sur la couverture du risque."
    )
    response = llm.invoke(prompt)
    return {"garanties_output": response}

def node_conformite(state: CreditState, config: RunnableConfig):
    params = config.get("configurable", {"worker_ctx": 2048, "worker_keep_alive": 0})
    
    llm = OllamaLLM(
        model="deepseek-r1:8b", 
        temperature=0.1,
        num_ctx=params["worker_ctx"],
        num_predict=256,
        keep_alive=format_keep_alive(params["worker_keep_alive"])
    )
    
    prompt = (
        f"Officier de Conformité:\n"
        f"Exécuter les vérifications réglementaires d'usage (KYC/AML) sur le dossier {state['dossier_id']}.\n"
        f"CONTRAINTE DE FORMAT STRICTE: Votre réponse DOIT commencer explicitement par l'un des drapeaux suivants:\n"
        f"soit 'STATUT: APPROUVE' soit 'STATUT: REFUS'. Justifier ensuite votre choix."
    )
    response = llm.invoke(prompt)
    return {"conformite_output": response}

def node_superviseur(state: CreditState, config: RunnableConfig):
    params = config.get("configurable", {"supervisor_ctx": 4096, "supervisor_keep_alive": 0})
    
    llm = OllamaLLM(
        model="deepseek-r1:14b", 
        temperature=0.0,
        num_ctx=params["supervisor_ctx"],
        num_predict=768,
        keep_alive=format_keep_alive(params["supervisor_keep_alive"])
    )
    
    prompt = (
        f"Directeur d'Engagement (Superviseur):\n"
        f"Consolider les rapports des 4 spécialistes ci-dessous pour formuler la décision d'octroi finale.\n\n"
        f"### Données de base:\n Dossier: {state['dossier_id']} | Montant: {state['montant_demande']}\n\n"
        f"### Analyse Solvabilité:\n {state['solvabilite_output']}\n\n"
        f"### Analyse Historique:\n {state['historique_output']}\n\n"
        f"### Analyse Garanties:\n {state['garanties_output']}\n\n"
        f"### Contrôle Conformité:\n {state['conformite_output']}\n\n"
        f"Rédiger un rapport de décision complet, clair et formalisé au format Markdown."
    )
    response = llm.invoke(prompt)

    
    return {
        "final_report": response,
        "solvabilite_output": "",
        "historique_output": "",
        "garanties_output": "",
        "conformite_output": ""
    }

# --- Dynamic Routing Logic ---
def check_compliance_route(state: CreditState):
    return "superviseur"

# --- State Graph Structural Assembler ---
builder = StateGraph(CreditState)

builder.add_node("solvabilite", node_solvabilite)
builder.add_node("historique", node_historique)
builder.add_node("garanties", node_garanties)
builder.add_node("conformite", node_conformite)
builder.add_node("superviseur", node_superviseur)

builder.add_edge(START, "solvabilite")
builder.add_edge("solvabilite", "historique")
builder.add_edge("historique", "garanties")
builder.add_edge("garanties", "conformite")

builder.add_conditional_edges(
    "conformite",
    check_compliance_route,
    {
        "superviseur": "superviseur"
    }
)
builder.add_edge("superviseur", END)

credit_app = builder.compile()
