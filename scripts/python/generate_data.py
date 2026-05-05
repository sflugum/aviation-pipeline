import pandas as pd
import numpy as np
import os
import json
from datetime import datetime, timedelta

# Configuration
START_DATE = datetime(2026, 4, 1)
END_DATE = datetime(2026, 4, 30, 23, 59)
NUM_AIRCRAFT = 75
TOTAL_MAINT_EVENTS = np.random.randint(200, 501) 

MODELS = [
    "Cessna Citation Latitude", "Embraer Phenom 300", "Gulfstream G650", 
    "Bombardier Challenger 350", "Dassault Falcon 2000", "Beechcraft King Air 350",
    "Boeing Business Jet", "Airbus ACJ319", "Pilatus PC-24", "HondaJet Elite"
]

AIRPORTS = ["CMH", "TEB", "VNY", "PBI", "DAL", "MDW", "LAS", "HPN", "APA", "ATL"]

MAINT_TYPES = {
    "Scheduled": ["Post-Flight Inspection", "Tire Pressure Check", "Oil Top-off", "Avionics Calibration"],
    "Unscheduled": ["Bird Strike Repair", "Hydraulic Leak", "FOD Damage", "Engine Flameout Inspection"]
}

if not os.path.exists('data'):
    os.makedirs('data')

# 1. Generate Aircraft
aircraft_list = []
for i in range(1, NUM_AIRCRAFT + 1):
    aircraft_list.append({
        "aircraft_id": i,
        "tail_number": f"N{100+i}QS",
        "model": np.random.choice(MODELS),
        "total_flight_hours": float(round(np.random.uniform(500, 5000), 2))
    })

# 2. Generate Flight Logs (Time-Sequenced per Aircraft)
all_flights = []
flight_id_counter = 1

for ac in aircraft_list:
    ac_id = ac["aircraft_id"]
    num_flights = np.random.randint(90, 151)
    target_monthly_hours = np.random.uniform(50, 81)
    avg_flight_duration = target_monthly_hours / num_flights
    
    current_time = START_DATE + timedelta(hours=np.random.randint(0, 12))
    
    for _ in range(num_flights):
        duration_hrs = max(0.5, np.random.normal(avg_flight_duration, 0.2))
        arrival_time = current_time + timedelta(hours=duration_hrs)
        
        if arrival_time > END_DATE:
            break
            
        dep_apt, arr_apt = np.random.choice(AIRPORTS, size=2, replace=False)
        
        all_flights.append({
            "flight_id": flight_id_counter,
            "aircraft_id": ac_id,
            "departure_airport": dep_apt,
            "arrival_airport": arr_apt,
            "departure_time": current_time.strftime('%Y-%m-%d %H:%M:%S'),
            "arrival_time": arrival_time.strftime('%Y-%m-%d %H:%M:%S')
        })
        
        flight_id_counter += 1
        current_time = arrival_time + timedelta(hours=np.random.uniform(1, 3))

# 3. Generate Maintenance Events (Distributed across fleet)
all_maint = []
for i in range(1, TOTAL_MAINT_EVENTS + 1):
    is_scheduled = np.random.random() > 0.15
    desc = np.random.choice(MAINT_TYPES["Scheduled" if is_scheduled else "Unscheduled"])
    base_cost = np.random.uniform(100, 1200) if is_scheduled else np.random.uniform(5000, 30000)
    
    all_maint.append({
        "event_id": i,
        "aircraft_id": int(np.random.choice([a["aircraft_id"] for a in aircraft_list])),
        "event_date": (START_DATE + timedelta(days=np.random.randint(0, 30))).strftime('%Y-%m-%d'),
        "description": desc,
        "cost": round(base_cost, 2)
    })

# --- FILE EXPORT ---

# Aircraft: Nested JSON 
aircraft_nested = {"fleet": aircraft_list}
with open('data/aircraft_source.json', 'w') as f:
    json.dump(aircraft_nested, f, indent=4)

# Flight Logs: JSON (List of dictionaries)
with open('data/flight_logs_source.json', 'w') as f:
    json.dump(all_flights, f, indent=4)

# Maintenance: CSV
pd.DataFrame(all_maint).to_csv('data/maintenance_events_source.csv', index=False)

print("Data generation complete.")