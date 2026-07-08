from typing_extensions import TypedDict

class CreditState(TypedDict):
    # Input variables passed into the graph
    dossier_id: str
    type_client: str
    montant_demande: str
    
    # Internal node data slots for holding intermediate worker results
    solvabilite_output: str
    historique_output: str
    garanties_output: str
    conformite_output: str
    
    # Final consolidated payload
    final_report: str