import sys
import os

# Append current working path to namespace registry
sys.path.append(os.getcwd())

from src.graph import credit_app

def print_menu():
    print("\n" + "="*50)
    print("      CONFIGURATEUR DE PERFORMANCE COMPLEMENTAIRE")
    print("="*50)
    print("[1] Mode FAST")
    print("[2] Mode FULL")
    print("="*50)

def get_allocation_config():
    while True:
        print_menu()
        choice = input("Sélectionnez le mode d'exécution (1-2) : ").strip()
        
        if choice == "1":
            print("\n[+] Mode FAST sélectionné. Configuration d'exécution standard.")
            return {
                "configurable": {
                    "worker_ctx": 2048,
                    "worker_keep_alive": 0,   # Libère le modèle worker immédiatement
                    "supervisor_ctx": 4096,
                    "supervisor_keep_alive": 0   # Libère immédiatement le 14B à la fin
                }
            }
        elif choice == "2":
            print("\n[+] Mode FULL sélectionné. Ressources maximales allouées.")
            return {
                "configurable": {
                    "worker_ctx": 4096,
                    "worker_keep_alive": 300,  # Conserve en VRAM pendant toute la démo (5 min)
                    "supervisor_ctx": 8192,
                    "supervisor_keep_alive": 300
                }
            }
        else:
            print("[-] Sélection invalide. Veuillez entrer 1 ou 2.")

def main():
    # Obtenir la configuration matérielle choisie par l'utilisateur
    config_runtime = get_allocation_config()

    initial_state = {
        "dossier_id": "DEM-9942",
        "type_client": "Personne Morale",
        "montant_demande": "250000 TND"
    }
    
    print("\n[+] INITIALIZING LANGGRAPH FINTECH STATE MACHINE...")
    print(f"[+] PROCESSING ENGINE PATH FOR DOSSIER: {initial_state['dossier_id']}")
    
    # Passer la configuration matérielle au cycle d'exécution du graphe via le paramètre config
    final_state = credit_app.invoke(initial_state, config=config_runtime)
    
    print("\n[+] DECISION FINALE DU SUPERVISEUR (LANGGRAPH SYNTHESIS RENDERING) :")
    print(final_state.get("final_report", "ERREUR: Aucun rapport n'a pu être généré par le superviseur."))

if __name__ == "__main__":
    main()
