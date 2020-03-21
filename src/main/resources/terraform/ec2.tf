resource "aws_instance" "cratekube-ec2-instance" {
  ami = "ami-07ebfd5b3428b6f4d"
  instance_type = "t2.micro"
  subnet_id = aws_subnet.cratekube-subnet-1.id
  vpc_security_group_ids = ["${aws_security_group.cratekube-ssh-sg.id}"]
  key_name = aws_key_pair.cratekube-key-pair.key_name

  tags = {
    Name = "cratekube-ec2-instance-1"
  }
}

resource "aws_key_pair" "cratekube-key-pair" {
  key_name = "cratekube-key-pair"
  public_key = var.cratekube-public-key
}
