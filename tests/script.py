import os.path,subprocess
from subprocess import STDOUT,PIPE

def compile_java(java_file):
    subprocess.check_call(['javac', java_file])

def execute_java():
    libs = "C:\\Users\\lukas\\Documents\\mestrado\\algoritmos\\simulador\\neko.jar;.\\target\\classes\\"
    main = "lse.neko.Main"
    config = "simulation.config.script"

    
    cmd = ['java', '-cp', libs, main, config ]
    proc = subprocess.Popen(cmd, stdin=PIPE, stdout=PIPE, stderr=STDOUT)
    stdout,stderr = proc.communicate()
    
    print ('STDOUT, look at log files: ' + str(stdout) + '\n\n')

def change_config(n=8):
    filename = "simulation.config"
    filename_out = "simulation.config.script"

    fin = open(filename,"rt")
    fout = open(filename_out,"wt")
    
    output = ""
    for line in fin:
        stripped_line = line.strip()
        new_line = stripped_line.replace("16", str(n))
        output += new_line +"\n"

    fout.write(output)
    fin.close()
    fout.close()

def messageCount(filename):
    fin = open(filename, "rt")

    # Conta cada linha do arquivo
    count = 0
    for line in fin:
        count += 1
    fin.close()

    # Envio e recebimento
    return count/2


base = 2
messages = []
for i in range(3,11):
    n = base ** i

    filename = "atomic-{n}.mensagens-2.log".format(n=n)
    messages.append(messageCount(filename))

print(messages)
    

#   print("###  Executando para {n} processos  #####".format(n=n))
#   change_config(n)
#   execute_java()

