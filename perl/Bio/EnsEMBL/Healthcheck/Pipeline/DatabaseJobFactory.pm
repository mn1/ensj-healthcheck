use strict;
use warnings;
package Bio::EnsEMBL::Healthcheck::Pipeline::DatabaseJobFactory;
use Bio::EnsEMBL::Hive::Utils qw/go_figure_dbc/;
use base ('Bio::EnsEMBL::Hive::RunnableDB::JobFactory');  # All Hive databases configuration files should inherit from HiveGeneric, directly or indirectly
use Data::Dumper;
use Bio::EnsEMBL::ApiVersion;

sub run {
    my $self = shift @_;
    # start a new session
    my $hc_dbc = go_figure_dbc($self->param('hc_conn'));
    my $session_id;
    $hc_dbc->sql_helper->execute_update(
        -SQL=>"insert into session(start_time,db_release) values(NOW(),?)", 
        -PARAMS=>[software_version()],
        -CALLBACK => sub {
            my ( $sth, $dbh, $rv ) = @_;
            $session_id = $dbh->{mysql_insertid};
            return;
        });
    $self->warning("Started new session $session_id");   
    my $output_ids = [];
    my $n = 0;
    for my $sql (
        q/select concat(species.db_name,"_",db.db_type,"_",db.db_release,"_",db.db_assembly) 
from ensembl_production.division join ensembl_production.division_species using (division_id) 
join ensembl_production.species using (species_id) 
join ensembl_production.db using (species_id) 
where division.shortname=? and db.is_current=1/,
        q/select db_name from ensembl_production.division join ensembl_production.division_db using (division_id) where shortname=? and is_current=1/
        ) {
        for my $db (grep {$_ !~ m/_mart_/} @{$hc_dbc->sql_helper()->execute_simple(-SQL=>$sql,-PARAMS=>[$self->param('division')])}) {
            push @$output_ids,{dbname=>$db,session_id=>$session_id};
            last if $n++>5;
        }        
    }    
    $self->warning("Processing ".scalar(@$output_ids)." databases");
    $self->param('output_ids',$output_ids);
    $self->param('session_id',$session_id);
    return;

}

sub write_output {
    my $self = shift @_;    
    my $output_ids = $self->param('output_ids');
    print "Writing output ids\n";
    $self->dataflow_output_id($output_ids, 2);
    $self->dataflow_output_id({session_id=>$self->param('session_id')}, 1);
    return 1;
}

1;
