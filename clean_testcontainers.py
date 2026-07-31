import os
import re

files = [
    'src/test/java/com/parkflow/infra/MigrationTest.java',
    'src/test/java/com/parkflow/inventory/infra/ParkingLotRepositoryTest.java',
    'src/test/java/com/parkflow/inventory/infra/SpotRepositoryTest.java',
    'src/test/java/com/parkflow/ParkflowApplicationTests.java',
    'src/test/java/com/parkflow/reservation/application/IdempotencyTest.java',
    'src/test/java/com/parkflow/reservation/application/ReservationServiceRaceTest.java',
    'src/test/java/com/parkflow/sensor/application/SensorEventConsumerTest.java'
]

for f in files:
    with open(f, 'r', encoding='utf-8') as file:
        content = file.read()
    
    content = content.replace('@Testcontainers(disabledWithoutDocker = true)', '@org.springframework.context.annotation.Import(com.parkflow.TestcontainersConfiguration.class)')
    content = content.replace('@Testcontainers', '@org.springframework.context.annotation.Import(com.parkflow.TestcontainersConfiguration.class)')
    
    content = re.sub(r'\s*@Container\s*@ServiceConnection(?:\(name = "[^"]+"\))?\s*static final [^;]+;?', '', content)
    content = re.sub(r'\s*@Container\s*@ServiceConnection\s*static final [^;]+;?', '', content)
    
    content = re.sub(r'import org\.testcontainers[^\n]+;\n', '', content)
    content = re.sub(r'import org\.springframework\.boot\.testcontainers[^\n]+;\n', '', content)

    with open(f, 'w', encoding='utf-8') as file:
        file.write(content)
print('Done!')
