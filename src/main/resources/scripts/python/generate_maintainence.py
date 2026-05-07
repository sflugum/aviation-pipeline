import csv 
import random
from datetime import datetime, timedelta


# ICAO24 numbers via OpenSky database "aircraft-database-complete-2025-08.csv"
# 'ae59f8', 'ae5a8d', 'ae5a8d', 'ae5b8a' were selected to cause null or erratic data
WATCHLIST = [
    '343111', '343113', '343147', '343240', '343286', '343603', '343618',
    '344502', '344545', '345103', '345258', '392940', '392981', '392981', 
    '392a93', '392c07', '392cb8', '392e51', '393065', '393189', '393444',
    '39381b', '393aa1', '393dcf', '39456b', '3fee45', '3fee55', '3feedb',
    '3fef70', '3ff01b', '3ff106', '3ff22e', '3ff3db', '3ff4c4', '3ff59c',
    '3ff649', '3ff7e5', '3ff917', '3ffb7d', '400381', '4006aa', '400853',
    '4009d1', '400b2a', '400dce', '400ef5', '400fee', '40117f', '4011d7',
    '40139c', '40148e', '4014f3', '401612', 'a84fb1', 'a85035', 'a85729',
    'a85b5e', 'a860d0', 'a86521', 'a86552', 'a868ba', 'ab1551', 'ab1570',
    'ab183a', 'ac803c', 'ac8070', 'ac807a', 'acdc3a', 'acdc64', 'ad91df',
    'ad93a0', 'ae59f8', 'ae5a8d', 'ae5a8d', 'ae5b8a'
]

def generate_logs(num_records=75):
    service_types = ['Engine Inspection', 'Tire Replacement', 'Avionics Calibration',
                     'Oil Chnage', 'Brake Pad Service']
    statuses = ['Completed', 'Pending', 'Scheduled']

    with open('raw_maintenance_logs.csv', 'w', newline='') as f:
        writer = csv.writer(f)
        writer.writerow(['icao24', 'service_type', 'status', 'log_date'])

        for _ in range(num_records):
            icao = random.choice(WATCHLIST)
            service = random.choice(service_types)
            status = random.choice(statuses)
            days_ago = random.randint(0, 30)
            log_date = (datetime.now() - timedelta(days=days_ago)).strftime('%Y-%m-%d %H:%M:%S')

            writer.writerow([icao, service, status, log_date])

if __name__ == "__main__":
    generate_logs()
    print("Successfully generated raw_maintenance_logs.csv")
