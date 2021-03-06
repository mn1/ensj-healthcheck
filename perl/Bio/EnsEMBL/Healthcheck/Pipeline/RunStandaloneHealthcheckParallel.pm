use strict;
use warnings;

package Bio::EnsEMBL::Healthcheck::Pipeline::RunStandaloneHealthcheckParallel;
use base ('Bio::EnsEMBL::Hive::Process');

use Bio::EnsEMBL::Hive::Utils::URL;
use Bio::EnsEMBL::ApiVersion;

use Carp qw/croak/;
use Data::Dumper;
use File::Slurp qw/read_file/;
use File::Temp qw/tempfile/;
use JSON;
use Log::Log4perl qw/:easy/;
use Bio::EnsEMBL::ApiVersion;

my $logger = get_logger();
if(!Log::Log4perl->initialized()) {
  Log::Log4perl->easy_init($DEBUG);
}

# Run a single HC against a single DB
sub run {

    my $self = shift @_;

    my $hc_jar = $self->param('hc_jar');
    my $java_opts = $self->param('java_opts') || '';
    
    my $db_uri = $self->param_required('db_uri');
    my $db = Bio::EnsEMBL::Hive::Utils::URL::parse($db_uri)->{dbname};
    my $hc_name = $self->param('hc_name');
    croak "Full HC name must be specified e.g. org.ensembl.healthcheck.testcase.WhatEvs rather than WhatEvs" unless $hc_name =~ m/org.ensembl/;

    my $fail_file = sprintf("./%s_%s.json", $db, $hc_name);

    my $command = sprintf("java $java_opts -jar %s --output_format json --output_file %s --release %s --t %s", $hc_jar, $fail_file, software_version(), $hc_name);

    $command .= get_db_str( $self->param_required('db_uri'));
    $command .= get_db_str($self->param('production_uri'), 'prod_');
    $command .= get_db_str($self->param('compara_uri'), 'compara_');
    $command .= get_db_str($self->param('live_uri'), 'secondary_');
    $command .= get_db_str($self->param('staging_uri'), 'staging_');
    $command .= sprintf(" --data_files_path %s",$self->param('data_files_path'));
    my $release = $self->param('release');
    if(!defined $release) {
		$release = software_version();    	
    }
    $command .= sprintf(" --release %d", $release);
    if(defined $self->param('master_schema')) {
    		$command .= sprintf(" --master_schema %s",$self->param('master_schema'));
    }

    my (undef,$log_file) = tempfile('_HealthcheckDatabase_XXXXXX',  SUFFIX => '.log', TMPDIR => 1, OPEN=>0);
    $command .= " >& $log_file";

    my $hive_dbc = $self->dbc;
    $hive_dbc->disconnect_if_idle() if defined $hive_dbc;

    $logger->info($command);
    my $exit = system($command);
    $exit >>= 8;
    $logger->info("Exited with status $exit");
    my $output = {
			       db_uri=>$db_uri,
			       hc_name=>$hc_name
			      };
    if($exit == 0) {
      $output->{status} = 'passed';
    } elsif($exit == 1) {
      $output->{status} = 'failed';
      eval {
        my $msg = decode_json(read_file($fail_file));
        print Dumper($msg);
        unlink $fail_file;
        $output->{messages} = $msg->{$db}->{$hc_name};
      }
      or do {
        my $log = read_file($log_file);
        croak "Could not execute $command: $log";
      };
    } else {
      my $log = read_file($log_file);
      croak "Could not execute $command: $log";
    }
    unlink $log_file;    
    $logger->debug(Dumper($output));
    $self->dataflow_output_id({
			       hc_output=>$output
			      }, 2);
    return;

}

sub get_db_str {
  my ($uri, $suffix) = @_;
  return '' unless defined $uri;
  $suffix ||= '';
  my $db = Bio::EnsEMBL::Hive::Utils::URL::parse($uri);
  my $db_str = sprintf(" --%shost %s --%sport %s --%suser %s", $suffix, $db->{host}, $suffix, $db->{port}, $suffix, $db->{user});
  if(defined $db->{pass} && $db->{pass} ne  '') {
    $db_str .= sprintf(" --%spass %s", $suffix, $db->{pass});
  }
  if(defined $db->{dbname} && $db->{dbname} ne  '') {
    $db_str .= sprintf(" --%sdbname %s", $suffix, $db->{dbname});
  }
  return $db_str;
}

1;
